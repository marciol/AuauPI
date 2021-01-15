(ns auaupi.routes
  (:require
   [auaupi.logic :as logic]
   [auaupi.not-logic :as not-logic]
   [auaupi.datomic :as datomic]
   [io.pedestal.interceptor :as io]
   [io.pedestal.http :as http]
   [pedestal-api.helpers :refer [before defbefore defhandler handler]]
   [route-swagger.doc :as sw.doc]
   [auaupi.config :as config]
   [schema.core :as s]))

(s/defschema Dog
  {:id s/Int
   :name s/Str
   :breed s/Str
   :image s/Str
   :birth s/Str
   :gender (s/enum "m" "f")
   :port (s/enum "p" "m" "g")
   :castrated? s/Bool
   :adopted? s/Bool})

(defn get-dogs [ctx]
  (let [result (-> config/config-map
                 datomic/open-connection
                 datomic/find-dogs
                 logic/datom->dog
                 http/json-response)]
    (assoc ctx :response result)))

(defn post-dogs-handler [req]
  (-> req
    (not-logic/check-breed! config/config-map)
    (not-logic/valid-dog! config/config-map)
    (datomic/transact-dog! config/config-map))
  (http/json-response {:status 200 :body "Registered Dog"}))

(defn post-adoption-handler [req]
  (-> req
    :path-params
    :id
    Long/valueOf
    (not-logic/check-adopted!
      (datomic/open-connection config/config-map))))

(defn get-dog-by-id-handler [req]
  (-> req
    :path-params
    :id
    Long/valueOf
    (datomic/find-dog-by-id
      (datomic/open-connection config/config-map))
    logic/datom->dog-full
    logic/data->response))

(defn respond-hello [_req]
  {:status 200 :body "Servidor funcionando"})

(s/defschema Dog
  {:id s/Int
   :name s/Str
   :breed s/Str
   :image s/Str
   :birth s/Str
   :gender (s/enum "m" "f")
   :port (s/enum "p" "m" "g")
   :castrated? s/Bool
   :adopted? s/Bool})

;(def list-dogs-interceptor
;  (sw.doc/annotate
;    {:summary    "List all dogs available for adoption"
;     :responses  {200 {:body Dog}
;                  400 {:body "Not Found/Empty List"}}
;     :operationId ::list-dogs}
;    (io/interceptor
;      {:name  :list-dogs
;       :enter get-dogs})))

(def get-dog-route
  (sw.doc/annotate
    {:summary    "List all dogs available for adoption"
     :parameters {:path-params {:id s/Int}}
     :responses  {200 {:body Dog}
                  400 {:body "Not Found/Empty List"}}
     :operationId ::specific-dog}
    (io/interceptor
      {:name  ::response-specific-dog
       :enter get-dog-by-id-handler})))

(def post-dog-route
  (handler
    ::create-pet
    {:summary     "Add a dog to our adoption list"
     :parameters  {:body-params Dog}
     :responses   {201 {:body Dog}}
     :operationId ::create-dog}
    post-dogs-handler))

(def adopt-dog-route
  "Example of using the before helper"
  (before
    ::update-pet
    {:summary     "Update a pet"
     :parameters  {:path-params {:id s/Int}
                   :body-params Dog}
     :responses   {200 {:body s/Str}}
     :operationId ::adopt-dog}
    post-adoption-handler))


(defn get-dogs [ctx]
  ;(let [result (-> config/config-map
  ;               datomic/open-connection
  ;               datomic/find-dogs
  ;               logic/datom->dog
  ;               http/json-response)]
  ;  (assoc ctx :response result))
  (assoc ctx :response {:status 200 :body "Opaaaaaaaaaaaaaaa"}))

(def list-dogs-interceptor
  (sw.doc/annotate
    {:summary    "List all dogs available for adoption"
     :parameters {}
     :responses  {200 {:body s/Str}
                  400 {:body s/Str}}}
    (io/interceptor
      {:name  :list-dogs
       :enter get-dogs})))