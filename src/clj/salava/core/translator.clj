(ns salava.core.translator
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.propertied.properties :refer [properties->map map->properties]]
            [clojure.java.io :as io]
            [clojure.data]
            [salava.core.helper :refer [dump plugin-str]]
            [salava.core.util :as util]))


(def config (-> (io/resource "config/core.edn") slurp read-string))

(def plugins (cons :core (:plugins config)))

(def languages (:languages config))

(def default-lang :en)

(def lang-pairs (mapcat (fn [l] (map (fn [p] [l p]) plugins)) languages))

(def dict-file "resources/i18n/dict.clj")


;;;

(defn dict-kw [plugin]
 (keyword (clojure.string/replace (plugin-str plugin) #"/" "-")))


(defn prop-read [filename]
  (with-open [r (io/reader filename :encoding "UTF-8")]
    (doto (java.util.Properties.)
      (.load r))))


(defn prop-write [filename prop]
  (with-open [w (io/writer filename :encoding "UTF-8")]
      (.store prop w nil)))


(defn prop-file [lang plugin]
  (if (nil? lang)
    (str "resources/i18n/" (plugin-str plugin) ".properties")
    (str "resources/i18n/" (name lang) "/" (plugin-str plugin) "_" (name lang) ".properties")))

(defn existing [file]
  (when (.exists (io/file file))
    file))


(defn load-prop-file [lang plugin]
  (some-> (prop-file lang plugin)
          (existing)
          (prop-read)
          (properties->map true)))


(defn prop-data-changed? [lang plugin new-data]
  (let [old-data (load-prop-file lang plugin)
        df (clojure.data/diff old-data new-data)]
    (not (and (nil? (first df)) (nil? (second df))))))


(defn save-prop-file [lang plugin data]
  (let [out-file (prop-file lang plugin)]
    (when (prop-data-changed? lang plugin data)
      (io/make-parents out-file)
      (prop-write out-file (map->properties data)))))


(defn save-dict-file [dict dev-mode]
  (log/info "writing dict.clj file")
  (with-open [w (io/writer dict-file :encoding "UTF-8")]
    (.write w (pr-str dict)))
  dict)


(defn props-to-dict []
  (log/info "reading .properties files")
  (reduce (fn [coll [lang plugin]]
            (assoc-in coll [lang (dict-kw plugin)] (load-prop-file lang plugin))) {} lang-pairs))


(defn dict-to-props [dict]
  (log/info "writing .properties files")
  (doseq [[lang plugin] lang-pairs]
    (when (keys (get-in dict [lang (dict-kw plugin)]))
      (save-prop-file lang plugin (get-in dict [lang (dict-kw plugin)])))))


(defn combine-dicts [source target]
  (into (sorted-map)
        (reduce (fn [coll k]
                  (assoc coll k (or (k coll) (k source)))) target (keys source))))


(defn add-default-vals [dict]
  (log/info "merging new keys from default language")
  (reduce (fn [coll [lang plugin]]
            (let [p (dict-kw plugin)]
              (assoc-in coll [lang p] (combine-dicts (get-in coll [default-lang p]) (get-in coll [lang p])))))
          dict lang-pairs))

(defn touch-i18n [_]
  (-> (io/resource "salava/core/i18n.cljc")
      (io/file)
      (.setLastModified (System/currentTimeMillis))))

(defn translate [& args]
  (-> (props-to-dict)
      (add-default-vals)
      (save-dict-file false)
      (dict-to-props)
      (touch-i18n))
  (System/exit 0))

