(ns salava.core.layout
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok content-type]]
            [schema.core :as s]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [salava.core.helper :refer [dump plugin-str]]
            [salava.core.util :refer [get-site-url]]
            [salava.user.db :as u]
            [salava.badge.main :as b]
            [salava.page.main :as p]
            [salava.gallery.db :as g]
            [hiccup.page :refer [html5 include-css include-js]]
            salava.core.restructure))

(def asset-css
  ["/assets/bootstrap/css/bootstrap.min.css"
   "/assets/bootstrap/css/bootstrap-theme.min.css"
   "/assets/font-awesome/css/font-awesome.min.css"
   "/css/rateit/rateit.css"])

(def asset-js
  ["/assets/jquery/jquery.min.js"
   "/assets/bootstrap/js/bootstrap.min.js"
   "/js/ckeditor/ckeditor.js"])


(defn with-version [ctx resource-name]
  (let [version (get-in ctx [:config :core :asset-version])]
    (str resource-name "?_=" (or version (System/currentTimeMillis)))))


(defn css-list [ctx]
  (let [plugin-css (map #(str "/css/" (plugin-str %) ".css") (cons :core (get-in ctx [:config :core :plugins])))]
    (map #(with-version ctx %) (concat asset-css plugin-css))))


(defn js-list [ctx]
    (map #(with-version ctx %) (conj asset-js "/js/salava.js")))


(defn context-js [ctx]
  (let [ctx-out {:plugins         {:all (get-in ctx [:config :core :plugins])}
                 :user            (:user ctx)
                 :flash-message   (:flash-message ctx)
                 :site-url        (get-in ctx [:config :core :site-url])
                 :site-name       (get-in ctx [:config :core :site-name])
                 :base-path       (get-in ctx [:config :core :base-path])
                 :facebook-app-id (get-in ctx [:config :oauth :facebook :app-id])
                 :linkedin-app-id (get-in ctx [:config :oauth :linkedin :app-id])
                 :languages       (map name (get-in ctx [:config :core :languages]))}]
    (str "function salavaCoreCtx() { return " (json/write-str ctx-out) "; }")))


(defn include-meta-tags [ctx tags]
  (if tags
    (let [{:keys [title description image]} tags]
      [[:meta {:property "og:title" :content title}]
       [:meta {:property "og:description" :content description}]
       [:meta {:name "description" :content description}]
       [:meta {:property "og:image" :content (str (get-site-url ctx) "/" image)}]])))

(defn main-view
  ([ctx] (main-view ctx nil))
  ([ctx meta-tags]
   (html5
     [:head
      [:title (get-in ctx [:config :core :site-name])]
      [:meta {:charset "utf-8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:meta {:property "og:sitename" :content (get-in ctx [:config :core :site-name])}]
      (seq (include-meta-tags ctx meta-tags))
      (apply include-css (css-list ctx))
      [:link {:type "text/css" :href "/css/custom.css" :rel "stylesheet" :media "screen"}]
      [:link {:type "text/css" :href "/css/print.css" :rel "stylesheet" :media "print"}]
      [:link {:type "text/css", :href "https://fonts.googleapis.com/css?family=Halant:300,400,600,700|Dosis:300,400,600,700,800|Gochi+Hand|Coming+Soon|Oswald:400,300,700|Dancing+Script:400,700|Archivo+Black|Archivo+Narrow|Open+Sans:700,300,600,800,400|Open+Sans+Condensed:300,700|Cinzel:400,700&subset=latin,latin-ext", :rel "stylesheet"}]
      [:script {:type "text/javascript"} (context-js ctx)]]
     [:body
      [:div#app]
      "<!--[if lt IE 10]>"
      (include-js "/assets/es5-shim/es5-shim.min.js" "/assets/es5-shim/es5-sham.min.js")
      "<![endif]-->"
      (include-js "/assets/es6-shim/es6-shim.min.js" "/assets/es6-shim/es6-sham.min.js")
      (apply include-js (js-list ctx))
      (include-js "https://backpack.openbadges.org/issuer.js")])))


(defn main-response [ctx current-user flash-message meta-tags]
  (let [user (if current-user (u/user-information ctx (:id current-user)))]
    (-> (main-view (assoc ctx :user user :flash-message flash-message) meta-tags)
        (ok)
        (content-type "text/html; charset=\"UTF-8\""))))

(defn main [ctx path]
  (GET path []
    :no-doc true
    :summary "Main HTML layout"
    :current-user current-user
    :flash-message flash-message
    (main-response ctx current-user flash-message nil)))

(defn main-meta [ctx path plugin]
  (GET path []
    :no-doc true
    :path-params [id :- s/Any]
    :summary "Main with meta tags"
    :current-user current-user
    :flash-message flash-message
    (let [meta-tags (case plugin
                      :badge (b/meta-tags ctx id)
                      :page (p/meta-tags ctx id)
                      :user (u/meta-tags ctx id)
                      :gallery (g/meta-tags ctx id))]
      (main-response ctx current-user flash-message meta-tags))))
