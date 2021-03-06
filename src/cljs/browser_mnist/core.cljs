(ns browser-mnist.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [clojure.core.matrix :as m]
              [thi.ng.ndarray.core :as nd]

              [cortex.network :as net]
              [cortex.serialization :as cs]
              [cortex.core :as core]

              [browser-mnist.model :as model]
              [browser-mnist.canvas :as canvas]

              [goog.string :refer [format]]
              [cljs.pprint :refer [pprint]]
              ))

(enable-console-print!)
(m/set-current-implementation :thing-ndarray)

;; -------------------------
;; State
(def network* (atom nil))
(def classified* (atom nil))
(def loading* (atom true))
(def classifying* (atom false))

;; -------------------------
;; Neural Network

(defn load-network [network]
  (->> network
       (cs/read-network!)
       (reset! network*))
  (reset! loading* false))

(defn classify [num-array]
  (->> (first (net/run @network* [num-array]))
       (into [])
       (map vector (range 10))
       (sort-by second)
       (reverse)
       (first)
       (first)
       (reset! classified*)))

;; -------------------------
;; Style

(def btn-style {:margin-top "10px"
                :color "black"
                :background-color "#f4f4f4"
                :border "1px solid #777"
                :border-radius "3px"
                :display "inline-block"
                :margin-left "3px"
                :margin-right "3px"
                :cursor "pointer"
                :padding "5px"})

(def color-style {:width "100px"
                  :height "100px"
                  :margin "5"
                  :color "black"
                  :background-color "#f4f4f4"
                  :border "2px solid #e9e9e9"
                  :border-radius "3px"
                  :display "inline-block"
                  :margin-left "3px"
                  :margin-right "3px"
                  :padding "5px"})


;; -------------------------
;; Actions

(defn mean [coll]
  (/ (apply + coll) (count coll)))


;; Use pure cljs to resize image (super slow in browser, don't use)
(defn get-array-cljs []
  (let [out-width 28
        out-height 28
        rows (->> (array-seq (canvas/get-pixels) 0)
                    (partition 4)        ; array -> rgba
                    (map #(take 3 %))    ; drop the alpha
                    (map (fn [[r g b]] (/ (+ (* 0.3 r) (* 0.59 g) (* 0.11 b)) 255.0))) ; greyscale
                    (partition (Math/floor (/ (canvas/get-width) out-width))) ; downscale...
                    (map mean))
        cols (for [x (range out-width)]
               (->> (drop x rows)
                    (take-nth out-width)
                    (partition (Math/floor (/ (canvas/get-height) out-height)))
                    (map mean)))
        pixels (apply interleave cols)
        pixels-ary (-> pixels
                   (m/array)
                   (m/reshape [out-width out-height]))]
    (doseq [r (m/to-nested-vectors pixels-ary)]
      (println "[" (map #(format "% 2.2f" %) r) "]"))
    (classify pixels)))

(defn get-array []
  (reset! classifying* true)
  (let [small-canvas (.getElementById js/document "small-canvas")
        small-context (.getContext small-canvas "2d")
        _ (doto small-context
            (.clearRect 0 0 28 28)
            (.drawImage (canvas/get-canvas) 0 0 28 28))
        pixels (-> small-context
                   (.getImageData 0 0 28 28)
                   (.-data)
                   (array-seq 0))
        pixels-ary (->> pixels
                        (partition 4)        ; array -> rgba
                        (map #(take 3 %))    ; drop the alpha
                        (mapv (fn [[r g b]] (/ (+ (* 0.3 r) (* 0.59 g) (* 0.11 b)) 255.0))) ; greyscale
                        )]
    (classify pixels-ary)
    (reset! classifying* false)
    ;(doseq [r (m/to-nested-vectors pixels-ary)]
    ;  (println "[" (map #(format "% 2.2f" %) r) "]"))
    ))

;; -------------------------
;; Page

(defn home-page []
  [:div [:h2 "Welcome to browser-mnist"]
   [:div
    [:div {:style {:width "350px"}} "Draw a digit [0-9] in the box below and click \"Recognize\" to use a trained neural net to classify it. You may also use a convolutional neural net, however note it is slower to classify."]
    [:br]
    (if @loading*
      [:div "Loading network..."]
      (if @classifying*
        [:div "Classifying..."]
        [:div.actions
         [:div.network {:style {:margin-top "5px"}}
          "Network Type: "
          [:select
           {:value "basic"
            :on-change (fn [e]
                        (canvas/clear!)
                        (reset! loading* true)
                        (reset! classified* nil)
                        (if (= (.. e -target -value) "basic")
                          (model/get-basic-network load-network)
                          (model/get-conv-network load-network)))}
           ^{:key "basic"} [:option {:value "basic"} "basic"]
           ^{:key "convolutional"} [:option {:value "convolutional"} "convolutional"]]]
         [:div.actions {:style {:margin-bottom "5px"}}
          [:span {:style btn-style :on-click #(do (canvas/clear!) (reset! classified* nil))} "Clear"]
          [:span {:style btn-style :on-click #(do (reset! classifying* true)
                                                  (get-array))} "Recognize"]
          (when @classified*
            [:span {:style {:margin-left "5px"
                            :display "inline-block"}} " Result: " @classified*])]]))
    [canvas/canvas]
    [:canvas {:id "small-canvas"
              :style {:display "none"
                      :border "1px solid blue"
                      :background-color "black"}
              :width "28"
              :height "28"}]]])

;; -------------------------
;; Routing

(defn current-page []
  [:div [(session/get :current-page)]])

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(defn mount-root []
  (model/get-basic-network load-network)
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
