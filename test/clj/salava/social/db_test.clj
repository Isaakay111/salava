(ns salava.social.db-test
  (:require [salava.social.db :as db]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))

(def test-user {:id 1 :role "user" :private false})

(t/deftest-ctx main-test [ctx]
  
  (testing "Get Badge events"
    (let [connects (db/get-all-events ctx (:id test-user))]
      (is (=  1 connects))
     
      )))
;(migrator/run-test-reset)
(migrator/reset-seeds (migrator/test-config))
