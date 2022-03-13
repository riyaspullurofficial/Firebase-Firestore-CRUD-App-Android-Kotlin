package com.example.firestorecrudapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.firestorecrudapp.databinding.ActivityMainBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    lateinit var binding:ActivityMainBinding
    private val personCollectionRef=Firebase.firestore.collection("persons")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.btnUploadData.setOnClickListener {
           try {
               val firstName=binding.etFirstName.text.toString()
               val lastName=binding.etLastName.text.toString()
               val age =binding.etAge.text.toString().toInt()

               if (false){
                   Toast.makeText(this@MainActivity,"Enter data...!!!",Toast.LENGTH_LONG).show()
               }else{
//                   val person=Person(firstName,lastName,age)
                   val person=getOldPerson()
                   savePerson(person)
                   binding.etFirstName.setText("")
                   binding.etLastName.setText("")
                   binding.etAge.setText("")
               }
           }catch (e:Exception){
               Log.d("Error",e.toString())
               Toast.makeText(this@MainActivity,"Enter data...!!!",Toast.LENGTH_LONG).show()
           }
        }
        subscribeRealtimeupdates()

        binding.btnRetrieveData.setOnClickListener {
            retrievePersons()
        }
        binding.updateBtn.setOnClickListener {
            val oldPerson=getOldPerson()
            val newPersonMap=getNewPersonMap()
            updatePerson(oldPerson,newPersonMap)

            subscribeRealtimeupdates()

            binding.newEtFirstName.setText("")
            binding.newEtLastName.setText("")
            binding.NewEtAge.setText("")
        }
        binding.deletePersonBtn.setOnClickListener {
            val person=getOldPerson()
            deletePerson(person)
        }

    }
    private fun savePerson(person: Person)= CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"Successfully saved data...!!!",Toast.LENGTH_LONG).show()
            }

        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }


    //button click data full show function
    private fun retrievePersonsOne()= CoroutineScope(Dispatchers.IO).launch {
        try {
            val querySnapshot=personCollectionRef.get().await()
            val sb=StringBuilder()
            for(document in querySnapshot.documents){
//                val person=document.toObject(Person::class.java)
                val person=document.toObject<Person>()
                sb.append("$person \n")
            }
            withContext(Dispatchers.Main){
                binding.tvPersons.text=sb.toString()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main){
               Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }


    //live changing
    private fun subscribeRealtimeupdates(){
        personCollectionRef.addSnapshotListener {querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {

                val sb=StringBuilder()
                for(document in it){
//                val person=document.toObject(Person::class.java)
                    val person=document.toObject<Person>()
                    sb.append("$person \n")
                }
                binding.tvPersons.text=sb.toString()
            }
        }
    }
    private fun retrievePersons()= CoroutineScope(Dispatchers.IO).launch {
        try {
            val fromAge=binding.etFrom.text.toString().toInt()
            val toAge=binding.etTo.text.toString().toInt()
            val querySnapshot=personCollectionRef
                .whereGreaterThan("age",fromAge)
                .whereLessThan("age",toAge)
                //.whereEqualTo("firstName","Riyas")
                .orderBy("age")
                .get()
                .await()
            val sb=StringBuilder()
            for(document in querySnapshot.documents){
//                val person=document.toObject(Person::class.java)
                val person=document.toObject<Person>()
                sb.append("$person \n")
            }
            withContext(Dispatchers.Main){
                binding.tvPersons.text=sb.toString()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun getOldPerson():Person{
        val firstName=binding.etFirstName.text.toString()
        val lastName=binding.etLastName.text.toString()
        val age =binding.etAge.text.toString().toInt()
        return Person(firstName,lastName,age)
    }
    private fun getNewPersonMap():Map<String,Any>{
        val firstName=binding.newEtFirstName.text.toString()
        val lastName=binding.newEtLastName.text.toString()
        val age=binding.NewEtAge.text.toString()
        val map= mutableMapOf<String,Any>()
        if (firstName.isNotEmpty()){
            map["firstName"]=firstName
        }
        if (lastName.isNotEmpty()){
            map["lastName"]=lastName
        }
        if (age.isNotEmpty()){
            map["age"]=age.toInt()
        }
        return map
    }
    private fun updatePerson(person: Person,newPersonMap: Map<String,Any>)= CoroutineScope(Dispatchers.IO).launch {
        val personQuery=personCollectionRef
            .whereEqualTo("firstName",person.firstName)
            .whereEqualTo("lastName",person.lastName)
            .whereEqualTo("age",person.age)
            .get()
            .await()
        if (personQuery.documents.isNotEmpty()){
            for (document in personQuery){
                try {
                    personCollectionRef.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    ).await()
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "no data matched query", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun deletePerson(person: Person)= CoroutineScope(Dispatchers.IO).launch {
        val personQuery=personCollectionRef
            .whereEqualTo("firstName",person.firstName)
            .whereEqualTo("lastName",person.lastName)
            .whereEqualTo("age",person.age)
            .get()
            .await()
        if (personQuery.documents.isNotEmpty()){
            for (document in personQuery){
                try {
                    personCollectionRef.document(document.id).delete().await()
                    //data to delete first name only
                   /* personCollectionRef.document(document.id).update(mapOf(
                        "firstName" to FieldValue.delete()
                    ))*/
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else{
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "no data matched query", Toast.LENGTH_LONG).show()
            }
        }
    }

}