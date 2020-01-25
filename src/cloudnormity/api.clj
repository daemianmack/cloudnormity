(ns cloudnormity.api
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cloudnormity.protocols :as p]))


(def ^:dynamic *tracking-attr* :cloudnormity/conformed)


(defn ensure-cloudnormity-schema
  [conn]
  (when-not (p/has-attr? (p/db conn) *tracking-attr*)
    (let [tx-data [{:db/ident *tracking-attr*
                    :db/valueType :db.type/keyword
                    :db/cardinality :db.cardinality/one
                    :db/doc "Conformed norm name"}]]
      (p/transact conn tx-data))))

(defn read-resource
  "Reads and returns data from a resource containing edn text."
  [resource-name]
  (with-open [reader (->> (io/resource resource-name)
                          io/reader
                          (java.io.PushbackReader.))]
    (edn/read reader)))

(defn eval-tx-fn
  [conn {:keys [tx-fn] :as norm-map}]
  (try ((requiring-resolve tx-fn) conn)
       (catch Throwable t
         (throw (ex-info (str "Exception evaluating " tx-fn)
                         {:exception t})))))

(defn tx-data-for-norm
  [conn {:keys [tx-data tx-fn tx-resource] :as norm-map}]
  (cond
    tx-data     tx-data
    tx-resource (read-resource tx-resource)
    tx-fn       (eval-tx-fn conn norm-map)))

(defn transact-norm
  [conn norm-map]
  (let [tx-data (into [{*tracking-attr* (:name norm-map)}]
                      (tx-data-for-norm conn norm-map))]
    (p/transact conn tx-data)))

(defn needed?
  [conn norm-map]
  (or (:mutable norm-map)
      (not (p/has-norm? (p/db conn) *tracking-attr* norm-map))))

(defn ensure-norms
  [conn norm-maps]
  ;; TODO Something more useful here than `nil` return,
  ;; report of succeeded/failed norms?
  (doseq [norm-map norm-maps]
    (when (needed? conn norm-map)
      (transact-norm conn norm-map))))

(defn ensure-conforms
  ([conn norm-maps]
   (ensure-conforms conn norm-maps (map :name norm-maps)))
  ([conn norm-maps norm-names]
   (ensure-cloudnormity-schema conn)
   (let [ensurable-norms (filter (comp (set norm-names) :name)
                                 norm-maps)]
     (ensure-norms conn ensurable-norms))))