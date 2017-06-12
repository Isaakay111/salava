(ns salava.core.i18n
  #?(:cljs (:require-macros [taoensso.tower :as tower-macros]
                            [salava.core.helper :refer [io-resource]]))
     (:require
       #?@(:cljs [[reagent.session :as session]
                  [salava.translator.ui.main :as tr]
                  [taoensso.tower :as tower :refer-macros (with-tscope)]])
       #?(:clj  [taoensso.tower :as tower :refer (with-tscope)])))

#?(:cljs
    (def iso-639 (-> (io-resource "i18n/iso-639.json") (js/JSON.parse) (js->clj :keywordize-keys true) :iso639)))

#?(:cljs
    (def lang-lookup (reduce (fn [coll lang] (assoc coll (:code lang) (:name lang))) {} iso-639)))

(def tconfig
  #?(:clj {:fallback-locale :en
           :dev-mode? true
           :dictionary "i18n/dict.clj"}
     :cljs {:fallback-locale :en
            :compiled-dictionary (tower-macros/dict-compile* "i18n/dict.clj")}))

(def translation (tower/make-t tconfig)) ; create translation fn

(defn get-t [lang key]
  (let [out-str (translation lang key)]
    (if-not (= out-str "")
      out-str
      (str "[" key "]"))))


#?(:clj  (defn t
           ([key] (get-t "en" key))
           ([key lng] (let [language (or lng "en")]
                         (get-t language key))))

   :cljs (defn t [& keylist]
           (let [lang (or (session/get-in [:user :language]) :en)]
             (if (session/get :i18n-editable)
               (tr/get-editable translation lang keylist)
               (apply str (map (fn [k] (if (keyword? k) (get-t lang k) k)) keylist))))))


(defn translate-text [text]
  (if (re-find #"/" text)
    (let [translated (if (keyword? text)
                       (t text)
                       (t (keyword text)))]
      (if (and (re-find #"\[" translated) (re-find #"\]" translated))
        text
        translated))
    text))
