(ns cloudnormity.api-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [clojure.set :as set]
            [clojure.string :as str]
            [cloudnormity.api :as sut]
            [cloudnormity.tx-sources :as tx-sources]
            [cloudnormity.specs :as s]
            [cloudnormity.util :as u]
            [cloudnormity.test-utils :as tu]
            [datomic.client.api :as d]))


(use-fixtures :each tu/test-system-fixture)


(def config (u/read-resource "sample-config.edn"))


(def all-idents-q
  '[:find ?ident
    :where
    [?e :db/ident ?ident]])

(defn all-idents
  [conn]
  (map first
       (d/q all-idents-q (d/db conn))))

(def conformed-norm-names-q
  '[:find ?name
    :in $ ?tracking-attr
    :where
    [?e ?tracking-attr ?name]])

(defn conformed-norm-names
  [conn]
  (map first
       (d/q conformed-norm-names-q
            (d/db conn)
            sut/*tracking-attr*)))

(def static-norm-maps
  "`tx-fn` norms can't be manipulated as data for testing purposes below
  because they require previous norms to have been transacted."
  (remove (comp #{:fix-user-zip} :name)
          config))

(defn norm-idents
  [conn norm-maps]
  (let [norm-maps (s/conform! norm-maps)
        tx-data (mapcat (partial tx-sources/tx-data-for-norm conn)
                        norm-maps)]
    (conj (keep :db/ident tx-data)
          sut/*tracking-attr*)))

(deftest ensure-conforms-basic
  (let [expected-norm-names (set (map :name config))
        idents-before (all-idents tu/*conn*)
        expected-new-idents (norm-idents tu/*conn* static-norm-maps)]
    (sut/ensure-conforms tu/*conn* config)
    (is (= (sort expected-norm-names)
           (sort (conformed-norm-names tu/*conn*))))
    (is (= (set expected-new-idents)
           (set/difference (set (all-idents tu/*conn*))
                           (set idents-before))))))

(deftest ensure-conforms-idempotency
  (let [tx-count #(count (d/tx-range tu/*conn* {:start 0 :end 1e6}))
        tracking-attr-count 1
        norm-map-count (count (map :name config))
        conformed-tx-count (+ norm-map-count
                              tracking-attr-count)
        t1-tx-count (tx-count)]
    (sut/ensure-conforms tu/*conn* config)
    (let [t2-tx-count (tx-count)]
      (is (= (+ t1-tx-count conformed-tx-count)
             t2-tx-count))
      (sut/ensure-conforms tu/*conn* config)
      (is (= t2-tx-count (tx-count))))))

(deftest ensure-conforms-specified-subset-of-norms
  (let [idents-before (all-idents tu/*conn*)
        expected-new-idents (norm-idents tu/*conn*
                                         (sut/norm-maps-by-name config [:base-schema]))]
    (sut/ensure-conforms tu/*conn* config [:base-schema])
    (is (= [:base-schema]
           (conformed-norm-names tu/*conn*)))
    (is (= (set expected-new-idents)
           (set/difference (set (all-idents tu/*conn*))
                           (set idents-before))))))

(deftest ensure-conforms-custom-tracking-attr
  (let [custom-attr :custom/tracking-attr
        expected-norm-names (set (map :name config))]
    (binding [sut/*tracking-attr* custom-attr]
      (sut/ensure-conforms tu/*conn* config)
      (is (= (sort expected-norm-names)
             (sort (conformed-norm-names tu/*conn*)))))))


(deftest ensure-conforms-respects-custom-tx-sources
  (let [idents-before (all-idents tu/*conn*)
        expected-new-idents [sut/*tracking-attr* :banans]
        schema [{:db/ident :banans
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one}]
        encode (comp str/reverse pr-str)
        decode (comp edn/read-string str/reverse)]
    (defmethod tx-sources/tx-data-for-norm :tx-banans
      [conn {[_ payload] :tx-source}]
      (decode payload))
    (sut/ensure-conforms tu/*conn*
                         [{:name :banans :tx-banans (encode schema)}]
                         [:banans])
    (remove-method tx-sources/tx-data-for-norm :tx-banans)
    (is (= (set expected-new-idents)
           (set/difference (set (all-idents tu/*conn*))
                           (set idents-before))))
    (is (= [:banans]
           (conformed-norm-names tu/*conn*)))))