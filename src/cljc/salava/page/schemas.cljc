(ns salava.page.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.badge.schemas :refer [Badge]]
            [salava.file.schemas :refer [File]]))

(def page
  {:id             s/Int
   :name           s/Str
   :description    (s/maybe s/Str)
   :tags           (s/maybe [s/Str])
   :password       (s/maybe s/Str)
   :visible_before (s/maybe s/Int)
   :visible_after  (s/maybe s/Int)
   :theme          (s/maybe s/Int)
   :ctime          s/Int
   :mtime          s/Int
   :padding        (s/maybe s/Int)
   :border         (s/maybe s/Int)
   :visibility     (s/enum "private" "password" "internal" "public")})

(s/defschema PageFile (-> File
                          (dissoc :ctime :mtime :tags)
                          (assoc :file_order s/Int)))

(s/defschema Page (assoc page :badges (s/maybe [(select-keys Badge [:name :image_file])])))

(s/defschema PageSettings (assoc page :user_id s/Int
                                      :first_name s/Str
                                      :last_name s/Str))

(s/defschema HeadingBlock {:type    (s/eq "heading")
                           :size    (s/enum "h1" "h2")
                           :content (s/maybe s/Str)})

(s/defschema BadgeBlock {:type     (s/eq "badge")
                         :format   (s/enum "short" "long")
                         :badge_id (s/maybe s/Int)})

(s/defschema HtmlBlock {:type     (s/eq "html")
                        :content (s/maybe s/Str)})

(s/defschema FileBlock {:type     (s/eq "file")
                        :files (s/maybe [PageFile])})

(s/defschema TagBlock {:type        (s/eq "tag")
                       :tag         (s/maybe (s/constrained s/Str #(and (>= (count %) 1)
                                                                        (<= (count %) 255))))
                       :format      (s/enum "short" "long")
                       :sort        (s/enum "name" "modified")})

(s/defschema ViewPage (assoc page :user_id s/Int
                                  :first_name s/Str
                                  :last_name s/Str
                                  :border {:id s/Int :style s/Str :width s/Int :color s/Str}
                                  :tags (s/maybe s/Str)
                                  :owner? s/Bool
                                  :qr_code (s/maybe s/Str)
                                  :blocks [(s/conditional #(= (:type %) "heading") (assoc HeadingBlock :id s/Int
                                                                                                       :block_order s/Int)
                                                          #(= (:type %) "badge") (merge
                                                                                   (assoc BadgeBlock :id s/Int
                                                                                                     :show_evidence (s/maybe s/Bool)
                                                                                                     :block_order s/Int)
                                                                                   (select-keys Badge [:name :html_content :criteria_url :description :image_file :issued_on :issuer_email :issuer_content_name :issuer_content_url :issuer_image :creator_email :creator_name :creator_url :creator_image :evidence_url :show_evidence]))
                                                          #(= (:type %) "html") (assoc HtmlBlock :id s/Int
                                                                                                 :block_order
                                                                                                 s/Int)
                                                          #(= (:type %) "file") (assoc FileBlock :id s/Int :block_order s/Int)
                                                          #(= (:type %) "tag") (assoc TagBlock :id s/Int
                                                                                               :block_order s/Int
                                                                                               :badges [(select-keys Badge [:id :name :html_content :criteria_url :description :image_file :issued_on :expires_on :visibility :mtime :status :badge_content_id :tag])]))]))

(s/defschema EditPageContent {:page   {:id          s/Int
                                       :user_id     s/Int
                                       :name        s/Str
                                       :description (s/maybe s/Str)
                                       :blocks      [(s/conditional #(= (:type %) "heading") (-> HeadingBlock
                                                                                                 (assoc :id s/Int :block_order s/Int)
                                                                                                 (dissoc :size))
                                                                    #(= (:type %) "sub-heading") (-> HeadingBlock
                                                                                                     (assoc :id s/Int :block_order s/Int :type (s/eq "sub-heading"))
                                                                                                     (dissoc :size))
                                                                    #(= (:type %) "badge") (-> BadgeBlock
                                                                                               (assoc :id s/Int
                                                                                                      :block_order s/Int
                                                                                                      :badge (select-keys Badge [:id :name :image_file]))
                                                                                               (dissoc :badge_id))
                                                                    #(= (:type %) "html") (assoc HtmlBlock :id s/Int
                                                                                                           :block_order
                                                                                                           s/Int)
                                                                    #(= (:type %) "file") (assoc FileBlock :id s/Int :block_order s/Int)
                                                                    #(= (:type %) "tag") (assoc TagBlock :id s/Int
                                                                                                         :block_order s/Int))]}
                              :badges [{:id         s/Int
                                        :name       s/Str
                                        :image_file (s/maybe s/Str)
                                        :tags       (s/maybe [s/Str])}]
                              :tags   (s/maybe [s/Str])
                              :files  [(dissoc PageFile :file_order)]})

(s/defschema SavePageContent {:name        (s/constrained s/Str #(and (>= (count %) 1)
                                                                      (<= (count %) 255)))
                              :description (s/maybe s/Str)
                              :blocks      [(s/conditional #(= (:type %) "heading") (assoc HeadingBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "badge") (assoc BadgeBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "html") (assoc HtmlBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "file") (assoc FileBlock (s/optional-key :id) s/Int
                                                                                                  :files (s/maybe [s/Int]))
                                                           #(= (:type %) "tag") (assoc TagBlock (s/optional-key :id) s/Int))]})
