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
(defn get-width [] 336)
(defn get-height [] 336)
(defn get-bounds [] [0 0 336 336])
(defn get-pixels [] (.-data (.getImageData @context* 0 0 336 336)))

;; -------------------------
;; Style
(def canvas-style {:border "1px solid black"
                   :border-radius "3px"
                   :background-color "#000000"
                   :cursor "pointer"
                   :width "336px"
                   :height "336px"})

;; -------------------------
;; Update Function

(defn clear! []
  (reset! clicks* [])
  (.clearRect @context* 0 0 (.-width @canvas*) (.-height @canvas*)))

(defn- redraw! [canvas context clicks]
  (.clearRect @context* 0 0 (.-width canvas) (.-height canvas))
  (set! (.-shadowBlur context) 10)
  (set! (.-shadowColor context) "#DDDDDD")
  (set! (.-strokeStyle context) "#FFFFFF")
  (set! (.-lineJoin context) "round")
  (set! (.-lineWidth context) 4)
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
                   :width "336"
                   :height "336"
                   :on-touch-start (fn [e]
                                     (let [touch (aget (.-touches e) 0)
                                           ; not sure why forwarding the event doesn't work, copy code here
                                           ;event-params (clj->js {:clientX (.-clientX touch)
                                           ;                       :clientY (.-clientY touch)})
                                           ;event (js/MouseEvent. "mousedown" event-params)
                                           pt [(- (.-clientX touch) (.-offsetLeft @canvas*))
                                               (- (.-clientY touch) (.-offsetTop @canvas*))]
                                           ]

                                       (swap! clicks* conj (conj pt false))
                                       (reset! dragging* true)
                                       (redraw! @canvas* @context* @clicks*)
                                       ;(.dispatchEvent @canvas* event)
                                     (.-preventDefault e)
                                     (.-stopPropagation e)
                                     ))
                   :on-touch-move (fn [e]
                                     (let [touch (aget (.-touches e) 0)
                                           ;event-params (clj->js {:clientX (.-clientX touch)
                                           ;                       :clientY (.-clientY touch)})
                                           ;event (js/MouseEvent. "mousemove" event-params)
                                           pt [(- (.-clientX touch) (.-offsetLeft @canvas*))
                                               (- (.-clientY touch) (.-offsetTop @canvas*))]
                                           ]
                                       (swap! clicks* conj (conj pt true))
                                       ;(.dispatchEvent @canvas* event))
                                       (redraw! @canvas* @context* @clicks*)
                                     (.-preventDefault e)
                                     (.-stopPropagation e)
                                     ))
                   :on-touch-end (fn [e]
                                     (let [;event (js/MouseEvent. "mouseup" (clj->js {}))
                                           ]
                                       ;(.dispatchEvent @canvas* event))
                                     (reset! dragging* false)
                                     (.-preventDefault e)
                                     (.-stopPropagation e)
                                     ))
                   :on-mouse-down (fn [e]
                                    (println "mouse down")
                                    (let [pt [(- (.-pageX e) (.-offsetLeft @canvas*))
                                            (- (.-pageY e) (.-offsetTop @canvas*))]]
                                      (swap! clicks* conj (conj pt false)))
                                    (reset! dragging* true)
                                    (redraw! @canvas* @context* @clicks*)
                                    (.-stopPropagation e))
                   :on-mouse-move (fn [e]
                                    (println "mouse move")
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
