(ns salava.user.db-test
  (:require [salava.user.db :as db]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))


(def test-user {:id 1 :role "user" :private false})

(def registration-data
  {:first_name "Testing"
   :last_name "Registration"
   :email "test.registration@example.com"
   :country "US"
   :password "123456"
   :language "fi"
   :password-verify "123456"})



(t/deftest-ctx main-test [ctx]
  (testing "register user "
    (testing "register user with correct data"
      (let [connect (db/register-user ctx (:email registration-data) (:first_name registration-data) (:last_name registration-data) (:country registration-data) (:language registration-data) (:password registration-data) (:password-verify registration-data))]
        (is (=  "success" (:status connect)))
        (is (=  "" (:message connect)))
        ))
    (testing "register user again with same data"
      (let [connect (db/register-user ctx (:email registration-data) (:first_name registration-data) (:last_name registration-data) (:country registration-data) (:language registration-data) (:password registration-data) (:password-verify registration-data))]
        (is (=  "error" (:status connect)))
        (is (=  "user/Enteredaddressisalready" (:message connect)))
        )))
  (testing "delete user (softdelete) "
    (let [registered-user (db/get-user-by-email ctx (:email registration-data))]
      (testing " delete created user"
        (let [connect (db/delete-user ctx (:id registered-user) (:password registration-data))]
          (is (= "success" (:status connect)))
          ))

      (testing " check can login with user"
        (let [login (db/login-user ctx (:email registration-data) (:password registration-data))]
          (is (= "error" (:status login)))
          (is (= "user/Loginfailed" (:message login)))))
      
      (testing " check can login with user"
        (let [emails (db/email-addresses ctx (:id registered-user))]
          (is (= "deleted-test.registration@example.com.so.deleted" (:email (first emails))))))
      
      ))
  
  (testing "register user and  verify email"
    (testing "register user with correct data"
      (let [connect (db/register-user ctx (:email registration-data) (:first_name registration-data) (:last_name registration-data) (:country registration-data) (:language registration-data) (:password registration-data) (:password-verify registration-data))]
        (is (=  "success" (:status connect)))
        (is (=  "" (:message connect)))
        ))

    (testing "activate email"
      (let [registered-user (db/get-user-by-email ctx (:email registration-data))
            connect  (db/verify-email-address ctx  (:verification_key registered-user) (:id registered-user) false)]
          (is (= "success"  connect))))

    (testing " check if activated"
      (let [registered-user (db/get-user-by-email ctx (:email registration-data))]
          (is (:verified registered-user)))))

  (testing "delete user (softdelete) "
    (let [registered-user (db/get-user-by-email ctx (:email registration-data))]
      (testing " delete created user"
        (let [connect (db/delete-user ctx (:id registered-user) (:password registration-data))]
          (is (= "success" (:status connect)))))

      (testing " check can login with user"
        (let [login (db/login-user ctx (:email registration-data) (:password registration-data))]
          (is (= "error" (:status login)))
          (is (= "user/Loginfailed" (:message login)))))
      
      (testing " check can login with user"
        (let [emails (db/email-addresses ctx (:id registered-user))]
          (is (= "deleted-test.registration@example.com.so.deleted" (:email (first emails))))))
      
      ))

  
  )

;(migrator/run-test-reset)
(migrator/reset-seeds (migrator/test-config))
