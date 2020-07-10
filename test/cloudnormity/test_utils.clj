(ns cloudnormity.test-utils
  (:require [clojure.test :as t]
            [datomic.client.api :as d]
            [datomic.dev-local :as dl])
  (:import [java.util UUID]
           [java.io File]))


(def ^:dynamic *conn*)


(defn db []
  (d/db *conn*))

(defn uuid []
  (UUID/randomUUID))

(defn init-test-db
  "Initializes a test DB and returns a connection"
  [client {:keys [db-name]}]
  (d/create-database client {:db-name db-name})
  (d/connect client {:db-name db-name}))

(defn make-client
  [client-opts]
  (d/client (merge {:server-type :dev-local}
                   client-opts)))

(defn storage-dir!
  []
  (let [dir (File. ".dev-local-storage")]
    (.mkdir dir)
    (.getAbsolutePath dir)))

(defn make-client-opts
  []
  {:system      (str "test-system-" (uuid))
   :db-name     (str "test-db-"     (uuid))
   :storage-dir (storage-dir!)})

(defn test-system-fixture
  [f]
  (let [client-opts (make-client-opts)
        db-client   (make-client client-opts)]
    (println "Creating test DB:" (:db-name client-opts))
    (try
      (let [conn (init-test-db db-client client-opts)]
        (binding [*conn* conn]
          (f)))
      (finally
        (println "Removing test DB:" (:db-name client-opts))
        (dl/release-db client-opts)))))

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

