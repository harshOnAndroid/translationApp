package com.flooent.translate

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flooent.translate.ConversationAdapter.ConversationHolder
import kotlinx.android.synthetic.main.chat_item_native.view.*

class ConversationAdapter(
    private var context: Context,
    conversation: ArrayList<Conversation>,
    private var showNativeLang : Boolean

) : RecyclerView.Adapter<ConversationHolder>() {
    private var conversationList= ArrayList<Conversation>()

    init{
        conversationList = ArrayList()
        conversationList.addAll(conversation)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationHolder {

        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return if (viewType == 1)
            ConversationHolder(View.inflate(context, R.layout.chat_item_native, null))
        else
            ConversationHolder(View.inflate(context, R.layout.chat_item_foreign, null))
    }

    override fun getItemViewType(position: Int): Int {
        return if (conversationList.get(position).isNative) 1 else 2
    }

    override fun getItemCount(): Int {
        return conversationList.size
    }

    override fun onBindViewHolder(holder: ConversationHolder, position: Int) {
        val conversation = conversationList[position]

        if (showNativeLang) {
            if (conversation.isNative)
                holder.txtSpeech.text = conversation.originalText
            else
                holder.txtSpeech.text = conversation.translatedText
        }else{
            if (conversation.isNative)
                holder.txtSpeech.text = conversation.translatedText
            else
                holder.txtSpeech.text = conversation.originalText
        }

        if (!showNativeLang)
        holder.txtSpeech.rotation = 180f

    }

    fun addNewMsg(newConversation: Conversation) {
        conversationList.add(newConversation)
        this.notifyItemInserted(conversationList.size - 1)
    }


    fun updateList(list:ArrayList<Conversation>){
        conversationList.clear()
        conversationList.addAll(list)
    }

    fun changeLanguage(showNativeLang:Boolean){
        this.showNativeLang = showNativeLang
        notifyDataSetChanged()
    }

    class ConversationHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        var txtSpeech: TextView = view.txt_speech

    }

}
