(ns cloudnormity.impl
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [datomic.client.api :as d]))

(defn tx!
  [conn tx-data]
  (d/transact conn {:tx-data tx-data}))

(defn has-attr?
  [conn attr-name]
  (-> (d/pull (d/db conn) '[:db/id] [:db/ident attr-name])
      :db/id
      boolean))

(def norm-q
  '[:find ?e
    :in $ ?tracking-attr ?norm-name
    :where [?e ?tracking-attr ?norm-name]])

(defn has-norm?
  [conn tracking-attr norm-map]
  (-> (d/q norm-q
           (d/db conn)
           tracking-attr
           (:name norm-map))
      seq
      boolean))

(defn ensure-cloudnormity-schema
  [conn tracking-attr]
  (when-not (has-attr? conn tracking-attr)
    (let [tx-data [{:db/ident tracking-attr
                    :db/valueType :db.type/keyword
                    :db/cardinality :db.cardinality/one
                    :db/doc "Conformed norm name"}]]
      (tx! conn tx-data))))

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
  "If the norm yields `tx-data`, transact it. Likeliest cause of a norm
  yielding no `tx-data`: a tx-fn that produces nothing, as in the case
  of a data-fix migration that has no useful work to perform."
  [conn norm-map tracking-attr]
  (let [tx-data (tx-data-for-norm conn norm-map)]
    (when (seq tx-data)
      (tx! conn (into [{tracking-attr (:name norm-map)}]
                      tx-data)))))

(defn needed?
  [conn norm-map tracking-attr]
  (or (:mutable norm-map)
      (not (has-norm? conn tracking-attr norm-map))))
