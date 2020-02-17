(ns cloudnormity.test-utils
  (:require [clojure.test :as t]
            [datomic.client.api :as d]
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

(defn submap?
  "Checks whether `m` contains all entries in `sub`."
  [m sub]
  {:pre [(map? m) (map? sub)]}
  (.containsAll (.entrySet ^java.util.Map m) (.entrySet ^java.util.Map sub)))

(defmethod t/assert-expr 'thrown-with-data? [msg form]
  ;; (is (thrown-with-data? {:k1 v1 :k2 v2} expr))
  ;; Asserts that evaluating `expr` throws an exception the `ex-data` of
  ;; which matches (via `submap` semantics) the map of expectations.
  (let [expect-map (second form)
        body (nthnext form 2)]
    `(try ~@body
          (t/do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
          (catch clojure.lang.ExceptionInfo e#
            (let [m# (ex-data e#)]
              (if (submap? m# ~expect-map)
                (t/do-report {:type :pass, :message ~msg,
                              :expected '~form, :actual m#})
                (t/do-report {:type :fail, :message ~msg,
                              :expected '~form, :actual m#})))
            e#))))

