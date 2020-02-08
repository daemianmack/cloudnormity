(ns cloudnormity.test-utils
  (:require [datomic.client.api :as d]
            [datomic.dev-local :as dl])
  (:import [java.util UUID]))


(def ^:dynamic *conn*)


(defn db []
  (d/db *conn*))

(defn uuid []
  (UUID/randomUUID))

(defn init-test-db
  "Initializes a test DB and returns a connection"
  [client db-name]
  (d/create-database client {:db-name db-name})
  (let [conn (d/connect client {:db-name db-name})]
    #_(retry-on-anomaly (db/transact-schema conn))
    conn))


(defn test-system-fixture
  [f]
  (let [system-name (str "test-system-" (uuid))
        _ (dl/add-system system-name nil)
        db-client (d/client {:system system-name})
        db-name (str "test-db-" (uuid))]
    (println "Test DB: " db-name)
    (try
      (let [conn (init-test-db db-client db-name)]
        (binding [*conn* conn]
          (f)))
      (finally
        (println "Destroying test DB:" db-name)
        (dl/release-db system-name db-name)))))

(comment

 (dl/add-system "my-amazing-system" nil)

 (def client (d/client {:system "my-amazing-system"}))

 (d/create-database client {:db-name "DB"})

 (def conn (d/connect client {:db-name "DB"}))

 (d/transact conn {:tx-data (-> "tiny-schema.edn"
                                clojure.java.io/resource
                                slurp
                                clojure.edn/read-string)})
  )
