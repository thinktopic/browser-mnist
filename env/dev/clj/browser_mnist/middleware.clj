(ns browser-mnist.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.transit :refer  [wrap-transit-params wrap-transit-response wrap-transit-body]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      wrap-transit-params
      ;wrap-transit-body
      wrap-transit-response
      wrap-exceptions
      wrap-reload))
