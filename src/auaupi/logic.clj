(ns auaupi.logic
  (:require
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [io.pedestal.http :as http]
   [auaupi.db :as db]
   [clojure.spec.alpha :as s]
   [auaupi.specs :as specs]
   [clojure.edn :as edn]))

(defn filter-dogs [params coll]
  (filter (fn [dog] (and (= params (select-keys dog (keys params)))
                         (= {:adopted? false} (select-keys dog (keys {:adopted? false}))))) coll))

(defn response-all [coll]
  (map #(into {}
              {:id (:id %)
               :breed (:breed %)
               :name (:name %)
               :img  (:img  %)}) coll))

(defn req->treated [req]
  (into {}
        (map (fn [[k s]]
               [k (try (let [v (edn/read-string s)]
                         (if (or (number? v)
                                 (boolean? v))
                           v
                           s))
                       (catch Throwable ex
                         (println ex)
                         s))]))
        req))

(defn get-breed-image! [raca]
  (-> (str "https://dog.ceo/api/breed/" (clojure.string/lower-case raca) "/images/random")
      client/get
      :body
      (json/read-str :key-fn keyword)
      :message))

(defn add-fields
  [map]
  (hash-map
   :id "4"
   :name (:auaupi.specs/name map)
   :breed (:auaupi.specs/breed map)
   :age (:auaupi.specs/age map)
   :gender (:auaupi.specs/gender map)
   :port (:auaupi.specs/port map)
   :castrated? (:auaupi.specs/castrated? map)
   :img (:img map)
   :adopted? false))

(defn create-dog!
  [{:keys [breed] :as dog}]
  (let [image (get-breed-image! (::specs/breed dog))
        image-added (->> image
                         (assoc dog :img)
                         add-fields)]
    (swap! db/dogs conj image-added)
    (http/json-response image-added)))

(defn valid-dog?
  [dog]
  (if (s/valid? ::specs/dog dog)
    (create-dog! dog)
    {:status 400 :body (json/write-str {:message "Invalid Format"})}))

(defn get-date []
  (.format (java.text.SimpleDateFormat. "dd/MM/yyyy") (new java.util.Date)))

(defn dog->adopt [coll]
  (->> coll
       (into {})
       (map (fn [[k v]] [k v]))
       (into {})
       (#(assoc % :adopted? true :adoptionDate (get-date)))))

(defn response-adopted [coll]
  (let [dog (->> coll
                 (into {})
                 (map (fn [[k v]] [k v]))
                 (into {}))]
    (if (not (nil? (:name dog)))
      (cond (= (:gender dog) "M") (str "Parabéns, você acabou de dar um novo lar para o " (:name dog) "!")
            (= (:gender dog) "F") (str "Parabéns, você acabou de dar um novo lar para a " (:name dog) "!"))
      "Parabéns! Adoção realizada com sucesso")))

