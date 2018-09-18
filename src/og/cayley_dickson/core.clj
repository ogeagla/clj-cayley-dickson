(ns og.cayley-dickson.core
  (:gen-class)
  (:import (org.apache.commons.math3.complex Complex)))

; inspired by https://github.com/hamiltron/py-cayleydickson/blob/master/cayleydickson.py

(defn -main
  [& args]
  (println "Hello, World!"))

(defprotocol Nion
  (init [this])
  (c [this])
  (times [this other])
  (neg [this])
  (plus [this other])
  (minus [this other])
  (valid-idx? [this idx])
  (get-idx [this idx])
  (set-idx [this idx new-val]))

(defprotocol NionOps
  (mag [this])
  (scale [this s])
  (norm [this])
  (inv [this])
  (rot [this other]))


(defn- nion-ops-mag [this]
  (->
    this
    norm
    Math/sqrt))

(defn- nion-ops-scale [this s]
  (loop [new-this this
         idx      (dec (:order this))]
    (let [new-new-this (set-idx
                         new-this
                         idx
                         (*
                           s
                           (get-idx new-this idx)))]
      (if (pos? idx)
        (recur new-new-this (dec idx))
        new-new-this))))

(defn- nion-ops-norm [this]
  (loop [sum 0
         idx (dec (:order this))]
    (let [new-sum (+
                    sum
                    (* (get-idx this idx)
                       (get-idx this idx)))]
      (if (pos? idx)
        (recur new-sum (dec idx))
        new-sum))))

(defn- nion-ops-inv [this]
  (scale (c this)
         (/ 1.0
            (norm this))))

(defn- nion-ops-rot [this other]
  (times
    (times other
           this)
    (inv other)))


(defn eq-order? [a b]
  (and
    (:order a)
    (:order b)
    (= (:order a)
       (:order b))))


(defrecord Complex2Apache [a b]
  Nion
  (init [this]
    (assoc this :order 2
                :obj (Complex. a b)))
  (c [this]
    (let [cpx-cnj (.conjugate (:obj this))]
      (assoc this :obj cpx-cnj
                  :a (.getReal cpx-cnj)
                  :b (.getImaginary cpx-cnj))))
  (neg [this]
    (let [cpx-neg (.negate (:obj this))]
      (assoc this :obj cpx-neg
                  :a (.getReal cpx-neg)
                  :b (.getImaginary cpx-neg))))
  (times [this other]
    (let [cpx-times (.multiply (:obj this) (:obj other))]
      (assoc this :obj cpx-times
                  :a (.getReal cpx-times)
                  :b (.getImaginary cpx-times))))
  (plus [this other]
    (let [cpx-add (.add (:obj this) (:obj other))]
      (assoc this :obj cpx-add
                  :a (.getReal cpx-add)
                  :b (.getImaginary cpx-add))))
  (minus [this other]
    (let [cpx-subtract (.subtract (:obj this) (:obj other))]
      (assoc this :obj cpx-subtract
                  :a (.getReal cpx-subtract)
                  :b (.getImaginary cpx-subtract))))
  (valid-idx? [this idx]
    (if-not
      (and
        (contains? #{0 1} idx)
        (:order this))
      (do (println "C2 Index must be int 0 or 1: " idx)
          false)
      true))

  (get-idx [this idx]
    (when (valid-idx? this idx)
      (if (< idx
             (/ (:order this)
                2))
        (:a this)
        (:b this))))
  (set-idx [this idx new-val]
    (when (valid-idx? this idx)
      (if (< idx
             (/ (:order this)
                2))
        (assoc this :a new-val
                    :obj (Complex. new-val (:b this)))
        (assoc this :b new-val
                    :obj (Complex. (:a this) new-val)))))
  NionOps
  (mag [this]
    (nion-ops-mag this))
  (scale [this s]
    (nion-ops-scale this s))
  (norm [this]
    (nion-ops-norm this))
  (inv [this]
    (nion-ops-inv this))
  (rot [this other]
    (nion-ops-rot this other)))

(defrecord Complex2 [a b]
  Nion
  (init [this]
    (assoc this :order 2))
  (c [this]
    (assoc this :b (* -1
                      (:b this))))
  (neg [this]
    (assoc this :a (* -1
                      (:a this))
                :b (* -1
                      (:b this))))

  (times [this other]
    (assoc this :a (- (* (:a this) (:a other))
                      (* (:b other) (:b this)))
                :b (+ (* (:a this) (:b other))
                      (* (:a other) (:b this)))))
  (plus [this other]
    (assoc this :a (+ (:a this)
                      (:a other))
                :b (+ (:b this)
                      (:b other))))
  (minus [this other]
    (plus this (neg other)))

  (valid-idx? [this idx]
    (if-not
      (and
        (contains? #{0 1} idx)
        (:order this))
      (do (println "C2 Index must be int 0 or 1: " idx)
          false)
      true))

  (get-idx [this idx]
    (when (valid-idx? this idx)
      (if (< idx
             (/ (:order this)
                2))
        (:a this)
        (:b this))))
  (set-idx [this idx new-val]
    (when (valid-idx? this idx)
      (if (< idx
             (/ (:order this)
                2))
        (assoc this :a new-val)
        (assoc this :b new-val))))
  NionOps
  (mag [this]
    (nion-ops-mag this))
  (scale [this s]
    (nion-ops-scale this s))
  (norm [this]
    (nion-ops-norm this))
  (inv [this]
    (nion-ops-inv this))
  (rot [this other]
    (nion-ops-rot this other)))

(defrecord Construction [a b]
  Nion
  (init [this]
    (if-not (eq-order? a b)
      (println "Orders don't match or missing" a b)
      (assoc this
        :order (* 2 (:order a)))))
  (c [this]
    (assoc this :a (c (:a this))
                :b (neg (:b this))))
  (times [this other]
    (if-not (eq-order? a b)
      (println "Orders don't match or missing" a b)
      (assoc this :a (minus
                       (times (:a this) (:a other))
                       (times (c (:b other)) (:b this)))
                  :b (plus
                       (times (:b other) (:a this))
                       (times (:b this) (c (:a other)))))))
  (neg [this]
    (assoc this :a (neg (:a this))
                :b (neg (:b this))))
  (plus [this other]
    (if-not (eq-order? a b)
      (println "Orders don't match or missing" a b)
      (assoc this :a (plus (:a this)
                           (:a other))
                  :b (plus (:b this)
                           (:b other)))))
  (minus [this other]
    (plus this (neg other)))
  (valid-idx? [this idx]
    (if-not (and
              (:order this)
              (= idx
                 (int idx))
              (>= idx 0)
              (<= idx (:order this)))
      (do (println
            "Construction Index must be int less than order: "
            idx (:order this))
          false)
      true))
  (get-idx [this idx]
    (when (valid-idx? this idx)
      (let [half-order (/ (:order this)
                          2)
            new-idx    (mod idx half-order)
            retval     (if (< idx half-order)
                         (get-idx (:a this) new-idx)
                         (get-idx (:b this) new-idx))]
        retval)))

  (set-idx [this idx new-val]
    (when (valid-idx? this idx)
      (let [half-order (/ (:order this)
                          2)
            new-idx    (mod idx half-order)]
        (if (< idx half-order)
          (assoc this :a (set-idx (:a this) new-idx new-val))
          (assoc this :b (set-idx (:b this) new-idx new-val))))))

  NionOps
  (mag [this]
    (nion-ops-mag this))
  (scale [this s]
    (nion-ops-scale this s))
  (norm [this]
    (nion-ops-norm this))
  (inv [this]
    (nion-ops-inv this))
  (rot [this other]
    (nion-ops-rot this other)))

(defn init-complex2 [a b impl]
  (case impl
    :plain (init
             (->Complex2 a b))
    :apache (init
              (->Complex2Apache a b))))

(defn init-construction [a b]
  (init
    (->Construction a b)))


