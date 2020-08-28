(ns salava.extra.customField.schemas
  (:require
   [schema.core :as s :include-macros true]))

(s/defschema gender* (s/enum "Male" "Female" "Notspecified"))
(s/defschema gender (s/maybe gender*))
(s/defschema organization* (s/constrained s/Str #(and (>= (count %) 1) (<= (count %) 255) (not (clojure.string/blank? %)))))
(s/defschema organization (s/maybe organization*))
