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
(defn get-conv-network [callback]
  (GET-array-buffer "/trained-network.conv" callback))

(defn get-basic-network [callback]
  (GET-array-buffer "/trained-network.basic" callback))
