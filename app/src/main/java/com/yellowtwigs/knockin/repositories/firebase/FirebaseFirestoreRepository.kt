package com.yellowtwigs.knockin.repositories.firebase

interface FirebaseFirestoreRepository {

    fun setActivityNameToUserClick(id: String, activityName: String)
    fun setActivityNameToUserClickBis(id: String, activityName: String)
}