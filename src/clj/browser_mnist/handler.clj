(ns browser-mnist.handler
  (:require [compojure.core :refer [GET DELETE PUT defroutes]]
            [compojure.route :refer [not-found resources]]
            [compojure.handler :as handler]

            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.util.http-response :refer [ok]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [hiccup.element :refer [javascript-tag]]
            [environ.core :refer [env]]
            [clojure.core.matrix :as m]

            [browser-mnist.middleware :refer [wrap-middleware]]

            [browser-mnist.browser-mnist :refer [train-network!
                                                 save-network!
                                                 classify]]))

(def mount-target
  [:div#app
      [:h3 "Loading the app."]
      [:p "please wait.. "]])

(defn loading-page []
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (javascript-tag (str "window.csrf_token=\"" *anti-forgery-token* "\";"))
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     mount-target
     (include-js "js/app.js")]]))



(defroutes routes
  (GET "/" [] (loading-page))

  (resources "/")
  (not-found "Not Found"))

(def app
  (wrap-middleware #'routes))
