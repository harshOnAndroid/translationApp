package com.flooent.translate

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flooent.translate.ConversationAdapter.ConversationHolder
import kotlinx.android.synthetic.main.chat_item_native.view.*

class ConversationAdapter constructor(
    private var context: Context,
    private var conversationList: ArrayList<Conversation>
) : RecyclerView.Adapter<ConversationHolder>() {
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

        holder.txtSpeech.text = conversationList[position].msg

    }

    fun addNewMsg(newConversation: Conversation){
        conversationList.add(newConversation)
        this.notifyItemInserted(conversationList.size-1)
    }

    class ConversationHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        var txtSpeech: TextView = view.txt_speech

    }

}
