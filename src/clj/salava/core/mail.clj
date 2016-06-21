(ns salava.core.mail
  (:require [clojure.java.io :as io]
            [hiccup.core :refer :all]
            [postal.core :refer [send-message]]
            [slingshot.slingshot :refer :all]
            [salava.core.i18n :refer [t]]))

(defn send-mail [ctx subject message recipients]
  (try+
    (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
          data {:from    (get-in ctx [:config :core :mail-sender])
                :to      recipients
                :subject subject
                :body    [{:type    "text/plain; charset=utf-8"
                           :content message}]}]
      (if (nil? mail-host-config)
        (send-message data)
        (send-message mail-host-config data)))
    (catch Object _
      ;TODO log an error
      )))

(defn send-activation-message [ctx site-url activation-link login-link fullname email-address lng]
  (let [subject (str (t :core/Emailactivation1 lng) "!")
        message (str fullname
                     ",\n\n" (t :core/Emailactivation2 lng) " " site-url ". "(t :core/Emailactivation3 lng) "\n" (t :core/Emailactivation4 lng) ":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5 lng) "\n" (t :core/Emailactivation6 lng) ".\n\n" (t :core/Emailactivation7 lng) "\n"
                     login-link
                     " " (t :core/Emailactivation8 lng) ".\n\n--  "(t :core/Obpteam lng))]
    (send-mail ctx subject message [email-address])))

(defn send-password-reset-message [ctx site-url activation-link fullname email-address]
  (let [subject (str (t :core/Emailresetheader))
        message (str fullname ",\n\n" (t :core/Emailresetmessage1) "\n" site-url
                     ".\n\n" (t :core/Emailactivation4)":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5) "\n" (t :core/Emailactivation6) ".\n\n" (t :core/Emailresetmessage2) ".\n\n--  "
                     (t :core/Obpteam))]
    (send-mail ctx subject message [email-address])))

(defn send-verification [ctx site-url email-verification-link fullname email]
  (let [subject (str (t :core/Emailverification1))
        message (str fullname "\n\n" (t :core/Emailverification2) " '" email "' " (t :core/Emailverification3) " " site-url".\n" (t :core/Emailverification4) ":\n\n"
                     email-verification-link
                     "\n\n" (t :core/Emailverification6)".\n")]
    (send-mail ctx subject message [email])))
