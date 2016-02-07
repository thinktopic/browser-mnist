(ns browser-mnist.model
  (:require
    [ajax.core :refer [GET DELETE PUT POST]]
    [cljs.core.async :refer [<! close! chan put!]]
    [cljs.pprint])
  (:refer-clojure :exclude [get remove]))

;;---------------------------------------------------
;; Helper Functions

(defn <<<
  "Turns an ajax function which takes a handler into a channel based call."
  [f & args]
  (let [c (chan)]
    (apply f (concat args [(fn [x]
                             (if (or (nil? x)
                                     (undefined? x))
                               (close! c)
                               (put! c x)))] [c]))
    c))

(defn error-handler [chan {:keys [status status-text response]}]
  (.log js/console (str "Error in model - status: " status " text: " status-text))
  (when (= status 400)
    (cljs.pprint/pprint response))
  (close! chan))

(def default-params* (atom {:error-handler error-handler
                            :response-format :transit
                            :headers {:x-csrf-token (aget js/window "csrf_token")}
                            :format :transit}))


(defn get [route params]
  (let [get- (fn [route params handler chan]
               (GET route {:handler handler :body params
                           :error-handler (partial (:error-handler @default-params*) chan)}
                    (merge @default-params*)))]
  (<<< get- route params)))

(defn put [route params]
  (let [put- (fn [route params handler chan]
               (PUT route {:handler handler :body params
                           :error-handler (partial (:error-handler @default-params*) chan)}
                    (merge @default-params*)))]
  (<<< put- route params)))


(defn delete [route params]
  (let [delete- (fn [route params handler chan]
               (DELETE route {:handler handler :body params
                           :error-handler (partial (:error-handler @default-params*) chan)}
                    (merge @default-params*)))]
  (<<< delete- route params)))


(defn GET-array-buffer [path callback]
  (let [request (js/XMLHttpRequest.)]
    (.open request "GET" path true)
    (set! (.-responseType request) "arraybuffer")
    (set! (.-onload request) (fn [] (callback (.-response request))))
    (.send request)))

;;---------------------------------------------------
;; Model Interface

(defn get-network [callback]
  (GET-array-buffer "/simple-trained-network" callback))

(defn get-nearest-colors [color callback]
  (PUT "/similar" {:handler callback
                   :headers {:x-csrf-token (aget js/window "csrf_token")}
                   :params {:color color}
                   :format :transit
                   :response-format :transit
                   :error-handler error-handler}))

(defn get-nearest-confusion [color callback]
  (PUT "/confusion" {:handler callback
                   :headers {:x-csrf-token (aget js/window "csrf_token")}
                   :params {:color color}
                   :format :transit
                   :response-format :transit
                   :error-handler error-handler}))

(defn update-color [color label callback]
  (PUT "/" {:handler callback
            :headers {:x-csrf-token (aget js/window "csrf_token")}
            :params {:color color :label label}
            :format :transit
            :response-format :transit
            :error-handler error-handler}))

(defn save [callback]
  (PUT "/save" {:handler callback
                :headers {:x-csrf-token (aget js/window "csrf_token")}
                :format :transit
                :response-format :transit
                :error-handler error-handler}))

(defn remove-color [color callback]
  (DELETE "/" {:handler callback
               :headers {:x-csrf-token (aget js/window "csrf_token")}
               :params {:color color}
               :format :transit
               :response-format :transit
               :error-handler error-handler}))

(defn retrain [callback]
  (GET "/retrain" {:handler callback
                   :response-format :transit
                   :error-handler error-handler}))
