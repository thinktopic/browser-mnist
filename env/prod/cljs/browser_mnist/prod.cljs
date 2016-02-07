(ns browser-mnist.prod
  (:require [browser-mnist.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
