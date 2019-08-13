package com.flooent.translate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeScreenViewModel : ViewModel() {
    private val conversationLiveData = MutableLiveData<List<Conversation>>()

    fun getUsers(): LiveData<List<Conversation>> {
        return conversationLiveData
    }

    fun setConversationList(list : ArrayList<Conversation>){
        conversationLiveData.value = list
    }
}