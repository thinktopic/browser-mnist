(ns browser-mnist.canvas
    (:require [reagent.core :as reagent :refer [atom]]
              [goog.string :refer [format]]))

;; -------------------------
;; State
(def canvas* (atom nil))
(def context* (atom nil))
(def clicks* (atom []))

;; -------------------------
;; Accessors
(defn get-canvas [] @canvas*)
(defn get-context [] @context*)
(defn get-width [] 364)
(defn get-height [] 364)
(defn get-bounds [] [0 0 364 364])
(defn get-pixels [] (.-data (.getImageData @context* 0 0 364 364)))

;; -------------------------
;; Style
(def canvas-style {:border "1px solid black"
                   :border-radius "3px"
                   :background-color "#000000"
                   :cursor "pointer"
                   :width "364px"
                   :height "364px"})

;; -------------------------
;; Update Function

(defn clear! []
  (reset! clicks* [])
  (.clearRect @context* 0 0 (.-width @canvas*) (.-height @canvas*)))

(defn- redraw! [canvas context clicks]
  (.clearRect @context* 0 0 (.-width canvas) (.-height canvas))
  (set! (.-shadowBlur context) 60)
  (set! (.-shadowColor context) "#DDDDDD")
  (set! (.-strokeStyle context) "#FFFFFF")
  (set! (.-lineJoin context) "round")
  (set! (.-lineWidth context) 10)
  (doseq [i (range (count clicks))]
    (.beginPath context)
    (if (get-in clicks [i 2])
      (.moveTo context (get-in clicks [(dec i) 0]) (get-in clicks [(dec i) 1]))
      (.moveTo context (dec (get-in clicks [i 0])) (get-in clicks [i 1])))
    (.lineTo context (get-in clicks [i 0]) (get-in clicks [i 1]))
    (.closePath context)
    (.stroke context)))

;; -------------------------
;; Component
(defn canvas []
  (let [dragging* (atom false)]
    (reagent/create-class
      {:component-did-mount
       (fn [this]
         (reset! canvas* (.getElementById js/document "canvas"))
         (reset! context* (.getContext @canvas* "2d")))
       :reagent-render
       (fn []
         [:canvas {:id "canvas"
                   :style canvas-style
                   :width "364"
                   :height "364"
                   :on-mouse-down (fn [e]
                                    (let [pt [(- (.-pageX e) (.-offsetLeft @canvas*))
                                            (- (.-pageY e) (.-offsetTop @canvas*))]]
                                      (swap! clicks* conj (conj pt false)))
                                    (reset! dragging* true)
                                    (redraw! @canvas* @context* @clicks*)
                                    (.-stopPropagation e))
                   :on-mouse-move (fn [e]
                                    (if @dragging*
                                      (let [pt [(- (.-pageX e) (.-offsetLeft @canvas*))
                                                (- (.-pageY e) (.-offsetTop @canvas*))]]
                                        (swap! clicks* conj (conj pt true))
                                        (redraw! @canvas* @context* @clicks*)))
                                    (.-stopPropagation e))
                   :on-mouse-up #(do (reset! dragging* false)
                                     (.-stopPropagation %))
                   :on-mouse-leave #(do (reset! dragging* false)
                                        (.-stopPropagation %))}])})))
