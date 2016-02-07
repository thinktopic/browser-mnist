(ns browser-mnist.browser-mnist
  (:require [clojure.java.io :as io]

            [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as rand]

            [cortex.protocols :as cp]
            [cortex.util :as util]
            [cortex.layers :as layers]
            [cortex.optimise :as opt]
            [cortex.core :as core]
            [cortex.serialization :as cs]
            [cortex.network :as net]
            [cortex.description :as desc]

            [thinktopic.datasets.mnist :as mnist])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream])
  (:import [mikera.vectorz Vectorz]))

(m/set-current-implementation :vectorz)

(defn- mnist-labels
    [class-labels]
    (let [n-labels (count class-labels)
          labels (m/zero-array [n-labels 10])]
        (doseq [i (range n-labels)]
            (m/mset! labels i (nth class-labels i) 1.0))
        labels))

(defonce trained-network* (atom nil))

;; Training
(defn train-network! []
  (let [;; Data
        training-data (into [] (m/rows @mnist/data-store))
        training-labels (into [] (m/rows (mnist-labels @mnist/label-store)))

        ;; Network Parameters
        hidden-layer-size 40
        n-epochs 10
        batch-size 10
        loss-fn (opt/mse-loss)
        network-desc [(desc/input 28 28 1)
                      (desc/convolutional 5 0 1 20)
                      (desc/max-pooling 2 0 2)
                      (desc/convolutional 5 0 1 50)
                      (desc/max-pooling 2 0 2)
                      (desc/linear->relu 500)
                      (desc/softmax 10)]
        built-network (desc/build-full-network-description network-desc)
        network (desc/create-network built-network)


        ;; more simple network, less epochs
        n-epochs 40
        input-width (last (m/shape training-data))
        output-width (last (m/shape training-labels))
        network-modules [(layers/linear-layer input-width hidden-layer-size)
                         (layers/logistic [hidden-layer-size])
                         (layers/linear-layer hidden-layer-size output-width)]
        network (core/stack-module network-modules)

        ; Use already trained network if it exists
        network (if @trained-network* @trained-network* network)

        ;; Newtork Modules
        optimizer (opt/adadelta-optimiser (core/parameter-count network))

        ;; Training
        trained-network (net/train network optimizer loss-fn training-data training-labels batch-size n-epochs)

        score (net/evaluate-softmax network training-data training-labels)]
    (println "score: " score)
    (reset! trained-network* trained-network))
  nil)

;; I/O
(defn save-network! []
  (let [network @trained-network*]
    (with-open [w (io/output-stream "resources/public/trained-network")]
      (cs/write-network! network w))))

(defn load-network! []
  (->> (io/resource "public/trained-network")
      (cs/read-network!)
      (reset! trained-network*)))
(defn show-first-number []
  (let [number-array (m/reshape (first @mnist/data-store) [28 28])]
    (doseq [r (m/to-nested-vectors number-array)]
      (println "[" (mapv #(if (> % 0.0)
                           (format "% 2.2f" %)
                           "     ") r) "]"))))

;; Running the network
(defn classify [number-input]
  (if-not @trained-network*
    (load-network!))
  (let [result (first (net/run @trained-network* [number-input]))
        max-res (apply max result)
        result-label (m/array (map #(if (= max-res %) 1.0 0.0) result))
        number-array (m/reshape (first @mnist/data-store) [28 28])]
    (doseq [r (m/to-nested-vectors number-array)]
      (println "[" (mapv #(format "% 2.2f" %) r) "]"))
    (println "resultz: " (first (first (reverse (sort-by second (map vector (range 10) result-label))))))
    result-label
    ))

