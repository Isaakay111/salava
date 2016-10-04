(ns salava.gallery.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.core.countries :refer [all-countries sort-countries]]
            [salava.page.main :as p]
            [salava.badge.main :as b]))

(defqueries "sql/gallery/queries.sql")

(defn public-badges-by-user
  "Return user's public badges"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (select-users-public-badges {:user_id user-id :visibility visibility} (get-db ctx))))



(defn public-badges
  "Return badges visible in gallery. Badges can be searched by country ID, badge name, issuer name or recipient's first or last name"
  [ctx country badge-name issuer-name recipient-name]
  (let [where ""
        params []
        default-visibility " AND (b.visibility = 'public' OR b.visibility = 'internal')"
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? badge-name)
                         [(str where " AND bc.name LIKE ?") (conj params (str "%" badge-name "%"))]
                         [where params])
        [where params] (if-not (empty? issuer-name)
                         [(str where " AND ic.name LIKE ?") (conj params (str "%" issuer-name "%"))]
                         [where params])
        [where params] (if-not (empty? recipient-name)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" recipient-name "%"))]
                         [where params])
        [where params] (if (and (not (empty? issuer-name)) (empty? recipient-name)) ;if issuer name is present but recipient name is not, search also private badges
                         [where params]
                         [(str where default-visibility) params])
        query (str "SELECT bc.id, bc.name, bc.image_file, bc.description, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime, badge_content_id  FROM badge AS b
                    JOIN badge_content AS bc ON b.badge_content_id = bc.id
                    JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
                    LEFT JOIN user AS u ON b.user_id = u.id
                    WHERE b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())"
                   where
                   " GROUP BY bc.id, bc.name, bc.image_file, bc.description, ic.name, ic.url, b.badge_content_id
                    ORDER BY ctime DESC
                    LIMIT 100")
        badgesearch (jdbc/with-db-connection
                  [conn (:connection (get-db ctx))]
                  (jdbc/query conn (into [query] params)))
        badge_contents (map :badge_content_id badgesearch)
        recipients (if (not-empty badge_contents) (select-badges-recipients {:badge_content_ids badge_contents } (get-db ctx)))
        recipientsmap (reduce #(assoc %1 (:badge_content_id %2) (:recipients %2)) {} recipients)
        assochelper (fn [user recipients] (assoc user  :recipients (get recipientsmap (:badge_content_id user))))]
    (map assochelper badgesearch recipients)))

(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn badge-countries
  "Return user's country id and list of all countries which users have public badges"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-badge-countries {} (into {:row-fn :country} (get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))

(defn page-countries
  "Return user's country id and list of all countries which users have public pages"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-page-countries {} (into {:row-fn :country} (get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))

(defn profile-countries [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-profile-countries {} (into {:row-fn :country} (get-db ctx)))]
    (-> all-countries
        (select-keys (conj countries current-country))
        (sort-countries)
        (seq))))

(defn public-badge-content
  "Return data of the public badge by badge-content-id. Fetch badge criteria and issuer data. If user has not received the badge use most recent criteria and issuer. Fetch also average rating of the badge, rating count and recipient count"
  [ctx badge-content-id user-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipient-badge-data (select-badge-criteria-issuer-by-recipient {:badge_content_id badge-content-id :user_id user-id} (into {:result-set-fn first} (get-db ctx)))
        badge-data (or recipient-badge-data (select-badge-criteria-issuer-by-date {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx))))
        rating (select-common-badge-rating {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipients (if user-id (select-badge-recipients {:badge_content_id badge-content-id} (get-db ctx)))
        badge (merge badge-content badge-data rating)]
    (hash-map :badge (b/badge-issued-and-verified-by-obf ctx badge)
              :public_users (->> recipients
                                 (filter #(not= (:visibility %) "private"))
                                 (map #(dissoc % :visibility))
                                 distinct)
              :private_user_count (->> recipients
                                       (filter #(= (:visibility %) "private"))
                                       count))))

(defn public-pages-by-user
  "Return all public pages owned by user"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (let [pages (select-users-public-pages {:user_id user-id :visibility visibility} (get-db ctx))]
     (p/page-badges ctx pages))))

(defn public-pages
  "Return public pages visible in gallery. Pages can be searched with page owner's name and/or country"
  [ctx country owner]
  
  (let [where ""
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? owner)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" owner "%"))]
                         [where params])
        query (str "SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
                    JOIN user AS u ON p.user_id = u.id
                    LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
                    WHERE (visibility = 'public' OR visibility = 'internal') AND p.deleted = 0"
                   where
                   " GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture
                    ORDER BY p.mtime DESC
                    LIMIT 100")
        pages (jdbc/with-db-connection
                [conn (:connection (get-db ctx))]
                (jdbc/query conn (into [query] params)))]
    (p/page-badges ctx pages)))

(defn public-profiles
  "Searcn public user profiles by user's name and country"
  [ctx search-params user-id]
  (let [{:keys [name country common_badges order_by]} search-params
        where ""
        order (case order_by
                "ctime" " ORDER BY ctime DESC"
                "name" " ORDER BY last_name, first_name"
                "")
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? name)
                         [(str where " AND CONCAT(first_name,' ',last_name) LIKE ?") (conj params (str "%" name "%"))]
                         [where params])
        query (str "SELECT id, first_name, last_name, country, profile_picture, ctime
                    FROM user
                    WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0"
                   where
                   order)
        profiles (jdbc/with-db-connection
                   [conn (:connection (get-db ctx))]
                   (jdbc/query conn (into [query] params)))
        common-badge-counts (if-not (empty? profiles)
                              (->>
                                (select-common-badge-counts {:user_id user-id :user_ids (map :id profiles)} (get-db ctx))
                                (reduce #(assoc %1 (:user_id %2) (:c %2)) {})))
        profiles-with-badges (map #(assoc % :common_badge_count (get common-badge-counts (:id %) 0)) profiles)
        visible-profiles (filter #(if common_badges
                                   (pos? (:common_badge_count %))
                                   identity) profiles-with-badges)]
    (if (= order_by "common_badge_count")
      (->> visible-profiles
           (sort-by :common_badge_count >)
           (take 100))
      (->> visible-profiles
           (take 100)))))

(defn meta-tags [ctx badge-content-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))]
    (rename-keys badge-content {:image_file :image :name :title})))
