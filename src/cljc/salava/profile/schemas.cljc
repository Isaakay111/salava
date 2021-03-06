(ns salava.profile.schemas
  #?(:clj (:require [schema.core :as s]
                    [compojure.api.sweet :refer [describe]]
                    [salava.core.countries :refer [all-countries]])
     :cljs (:require [schema.core :as s :include-macros true]
                     [salava.core.countries :refer [all-countries]])))

#?(:cljs (defn describe [v _] v))

(defn either [s1 s2]
 #?(:clj (s/either s1 s2)
    :cljs (s/cond-pre s1 s2)))

(def block-types ["pages" "badges" "location" "showcase"])

(def additional-fields
  [{:type "email" :key :user/Emailaddress}
   {:type "phone" :key :user/Phonenumber}
   {:type "address" :key :user/Address}
   {:type "city" :key :user/City}
   {:type "state" :key :user/State}
   {:type "country" :key :user/Country}
   {:type "facebook" :key :user/Facebookaccount}
   {:type "linkedin" :key :user/LinkedInaccount}
   {:type "twitter" :key :user/Twitteraccount}
   {:type "pinterest" :key :user/Pinterestaccount}
   {:type "instagram" :key :user/Instagramaccount}
   {:type "blog" :key :user/Blog}])

(s/defschema badge {:id                           s/Int
                    (s/optional-key :name)        s/Str
                    (s/optional-key :image_file ) (s/maybe s/Str)
                    (s/optional-key :description) (s/maybe s/Str)
                    (s/optional-key :visibility)  (s/enum "private" "public" "internal")})

(s/defschema user-setting {(s/optional-key :activated) (s/maybe s/Bool)
                           (s/optional-key :email_notifications) (s/maybe s/Bool)
                           (s/optional-key :private) (s/maybe s/Bool)})

(s/defschema profile-field {:id          s/Int
                            :field_order s/Int
                            :value       (s/maybe s/Str)
                            :field       (s/constrained s/Str #(some (fn [f] (= % f) ) (map :type additional-fields)))})

(s/defschema badge-showcase {(s/optional-key :id)          s/Int
                             :type                         (s/eq "showcase")
                             (s/optional-key :block_order) (describe s/Int "Block arrangement order")
                             :title                        (describe (s/maybe s/Str) "Title of the showcase when displayed in profile. Default name is Untitled")
                             :badges                       (describe [(s/maybe badge)] "ids of badges you want to add to your showcase")
                             :format                       (describe (s/eq "short") "Always short format for showcase badges")})

(s/defschema add-showcase-block {(s/optional-key :id)          s/Int
                                 :type                         (s/eq "showcase")
                                 :title                        (describe (s/maybe s/Str) "Title of the showcase when displayed in profile. Default name is Untitled")
                                 :badges                       (describe [s/Int] "ids of badges you want to add to your showcase")})


#_(def recent-badges-block  (describe {:block_order             s/Int
                                       :type                    (s/eq "badges")
                                       (s/optional-key :hidden) s/Bool}  "Default block, possible operations: hide, change display order in profile"))

#_(def recent-pages-block  (describe {:block_order             s/Int
                                      :type                    (s/eq "pages")
                                      (s/optional-key :hidden) s/Bool}  "Default block, possible operations: hide, change display order in profile"))

#_(def location-block  (describe {:block_order             s/Int
                                  :type                    (s/eq "location")
                                  (s/optional-key :hidden) s/Bool}  "Default block, Appears when user location is enabled. possible operations: hide, change display order in profile"))


(s/defschema profile-block (either
                            badge-showcase
                            {:block_order            s/Int
                             :type                   (apply s/enum block-types)
                             (s/optional-key :hidden) s/Bool}))


(s/defschema profile-tab {:id         (describe s/Int "user page id")
                          :name       (describe s/Str "page name")
                          :visibility (describe (s/enum "private" "public" "internal") "page visibility")})

(s/defschema picture-file {:id s/Int
                           :name s/Str
                           :path s/Str
                           :size s/Int
                           :mime_type s/Str
                           :ctime s/Int
                           :mtime s/Int})

(s/defschema user {:id                 s/Int
                   :role               (s/enum "user" "admin")
                   :first_name         (s/constrained s/Str #(and (>= (count %) 1)
                                                                  (<= (count %) 255)))
                   :last_name          (s/constrained s/Str #(and (>= (count %) 1)
                                                                  (<= (count %) 255)))
                   :country            (apply s/enum (keys all-countries))
                   :language           (s/enum "fi" "en" "fr" "es" "pl" "pt" "ar" "nl" "sv")
                   :profile_visibility (s/enum "public" "internal")
                   :profile_picture    (s/maybe s/Str)
                   :about              (s/maybe s/Str)})

(s/defschema current-user (-> user
                              (merge user-setting)
                              (assoc :email s/Str)))


(s/defschema user-profile {(s/optional-key :user)       (merge user user-setting)
                           (s/optional-key :profile)    [(s/maybe profile-field)]
                           :visibility (s/enum "public" "internal" "gone")
                           (s/optional-key :blocks)     [(s/maybe profile-block)]
                           (s/optional-key :theme)      s/Int
                           (s/optional-key :tabs)       [(s/maybe profile-tab)]
                           (s/optional-key :owner?)     s/Bool})

(s/defschema edit-user-profile-p  {(s/optional-key :profile_visibility) (s/enum "internal" "public")
                                   (s/optional-key :about)           (describe (s/maybe s/Str) "A short description about user")
                                   (s/optional-key :profile_picture) (describe (s/maybe s/Str) "picture url")
                                   (s/optional-key :theme)           (describe s/Int "Default theme is 0. Options 0 - 13")})


(s/defschema user-profile-for-edit {:user_id       s/Int
                                    :user          (-> user (select-keys [:about :profile_picture :profile_visibility]))
                                    :profile       [(s/maybe profile-field)]
                                    :picture_files [(s/maybe picture-file)]})

(s/defschema block-for-edit  (describe (s/conditional
                                         #(= (:type %) "showcase") badge-showcase
                                         #(= (:type %) "badges")   {:block_order s/Int :type (s/eq "badges") :hidden (s/maybe s/Bool)}
                                         #(= (:type %) "pages")    {:block_order s/Int :type (s/eq "pages") :hidden (s/maybe s/Bool)}
                                         #(= (:type %) "location") {:block_order s/Int :type (s/eq "location") (s/optional-key :hidden) (s/maybe s/Bool)}) "Badge blocks"))

(s/defschema fields [(s/maybe {:field (apply s/enum (map :type additional-fields)) :value (s/maybe s/Str)})])

(s/defschema edit-user-profile (assoc (-> user (select-keys [:profile_visibility :profile_picture :about]))
                                      (s/optional-key :fields) fields
                                      :blocks (describe [(s/maybe block-for-edit)]
                                                        "Default blocks include user's recently published badges and pages. User location is also added by default when user location is enabled.
                                                                          Badge showcases can be added as extra blocks. Model shows a badge showcase block being added")
                                      :theme (describe (s/maybe s/Int) "Default theme 0")
                                      :tabs  (describe [(s/maybe profile-tab)] "Pages created by the user can be added as profile tabs ")))

(s/defschema reorder-profile-resource {:type (describe (s/enum "tabs" "blocks" "fields") "resource to be reordered") :input (describe [s/Int] "ids in desired order. For type blocks, current block_order in desired order")})

#_(s/defschema ShowcaseBlock {:type (s/eq "showcase")
                              :title  (s/maybe s/Str)
                              :badges [{:id (s/maybe s/Int) (s/optional-key :visibility) s/Str}]
                              :format (s/enum "short" "medium" "long")})

#_(s/defschema BlockForEdit (s/conditional
                                     #(= (:type %) "showcase") (assoc ShowcaseBlock (s/optional-key :id) s/Int
                                                                (s/optional-key :block_order) s/Int)
                                     #(= (:type %) "badges") {:block_order s/Int :type (s/eq "badges") :hidden (s/maybe s/Bool)}
                                     #(= (:type %) "pages") {:block_order s/Int :type (s/eq "pages") :hidden (s/maybe s/Bool)}
                                     #(= (:type %) "location") {:block_order s/Int :type (s/eq "location") (s/optional-key :hidden) (s/maybe s/Bool)}))

#_(s/defschema EditProfile (assoc (-> user (select-keys [:profile_visibility :profile_picture :about]))
                                  :fields [{:field (apply s/enum (map :type additional-fields)) :value (s/maybe s/Str)}]
                                  :blocks [(s/maybe block-for-edit)]
                                  :theme (s/maybe s/Int)
                                  :tabs  [(s/maybe profile-tab #_{:id s/Int :name s/Str :visibility s/Str})]))
