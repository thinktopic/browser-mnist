(ns browser-mnist.model
  (:require))

;;---------------------------------------------------
;; Helper Functions


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

