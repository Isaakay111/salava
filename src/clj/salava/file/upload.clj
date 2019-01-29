(ns salava.file.upload
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [mime-type-of extension-for-name]]
            [salava.core.util :refer [public-path]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [unix-time]]
            [salava.file.db :as f]))

(defn upload-file [ctx user-id file image-only?]
  (try+
    (let [{:keys [size filename tempfile]} file
          types (-> ctx
                    (get-in [:config :file :allowed-file-types])
                    vals
                    distinct)
          mime-types (if image-only?
                       (filter #(re-matches #"^image/.*" %) types)
                       types)
          max-size (get-in ctx [:config :file :max-size] 100000000)
          mime-type (mime-type-of tempfile)
          extension (extension-for-name mime-type)
          path (public-path tempfile extension)
          data-dir (get-in ctx [:config :core :data-dir])
          full-path (str data-dir "/" path)
          insert-data {:name filename
                       :path path
                       :size size
                       :mime_type mime-type
                       :tags []}
          max-quota (get-in ctx [:config :file :max-quota] 100000000)
          used-quota (f/used-quota ctx user-id)]
      (if (> (+ used-quota size) max-quota)
        (throw+ "file/Exceededquota"))
      (if (> size max-size)
        (throw+ "file/Filetoobig"))
      (if-not (some #(= % mime-type) mime-types)
        (throw+ "file/isnotallowed"))
      (if-not (and data-dir (.exists (io/as-file data-dir)))
        (throw+ "Data directory does not exist"))

      (io/make-parents full-path)
      (io/copy tempfile  (io/file full-path))
      (let [file-id (f/save-file! ctx user-id insert-data)
            file-data (assoc insert-data :id file-id
                        :ctime (unix-time)
                        :mtime (unix-time))]
        {:status "success" :message "file/Fileuploaded" :reason "file/Fileuploaded" :data file-data}))
    (catch Object _
      {:status "error" :message "file/Errorwhileuploading" :reason _})
    (finally
      (.delete (:tempfile file)))))
