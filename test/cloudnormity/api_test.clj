(ns cloudnormity.api-test
  (:require [clojure.test :refer :all]
            [cloudnormity.api :as sut]))


;; TODO Our testing hack doesn't work with data changes, so remove
;; that norm from consideration for now.
(def config (remove
             #(= :fix-user-zip (:name %))
             (sut/read-resource "sample-config.edn")))


(deftest ensure-conforms-basic
  (let [db  (atom [])
        exp [sut/*tracking-attr* :schema/deprecated :schema/see-instead :user/id :user/tel :user/zip :user/zip-code]]
    (sut/ensure-conforms db config)
    (is (= exp (keep :db/ident @db)))
    (is (= (map :name config)
           (keep sut/*tracking-attr* @db)))))

(deftest ensure-conforms-idempotency
  (let [db  (atom [])
        exp [sut/*tracking-attr* :schema/deprecated :schema/see-instead :user/id :user/tel :user/zip :user/zip-code]]
    (sut/ensure-conforms db config)
    (sut/ensure-conforms db config)
    (is (= exp (keep :db/ident @db)))
    (is (= (map :name config)
           (keep sut/*tracking-attr* @db)))))

(deftest ensure-conforms-specified-subset-of-norms
  (let [db  (atom [])
        exp [sut/*tracking-attr* :schema/deprecated :schema/see-instead :user/id :user/tel]]
    (sut/ensure-conforms db config [:base-schema])
    (is (= exp (keep :db/ident @db)))
    (is (= [:base-schema]
           (keep sut/*tracking-attr* @db)))))

(deftest ensure-conforms-custom-tracking-attr
  (let [db   (atom [])
        attr :custom/tracking-attr
        exp  [attr :schema/deprecated :schema/see-instead :user/id :user/tel :user/zip :user/zip-code]]
    (binding [sut/*tracking-attr* attr]
      (sut/ensure-conforms db config (map :name config)))
    (is (= exp (keep :db/ident @db)))
    (is (= (map :name config)
           (keep attr @db)))))
