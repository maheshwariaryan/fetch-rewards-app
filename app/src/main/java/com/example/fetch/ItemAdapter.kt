package com.example.fetch

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(private val groupedItems: Map<Int, List<Item>>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    companion object {
        private const val TAG = "FetchApp"
    }

    private val listIdKeys = groupedItems.keys.sorted()

    init {
        Log.d(TAG, "ItemAdapter initialized with ${listIdKeys.size} groups")
        listIdKeys.forEach { listId ->
            val count = groupedItems[listId]?.size ?: 0
            Log.d(TAG, "Group $listId has $count items")
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupHeaderTextView: TextView = view.findViewById(R.id.textViewListId)
        val itemsRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewItems)

        init {
            Log.d(TAG, "ViewHolder initialized")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_group_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listId = listIdKeys[position]
        val items = groupedItems[listId] ?: emptyList()

        Log.d(TAG, "onBindViewHolder for group $listId with ${items.size} items")

        holder.groupHeaderTextView.text = "List ID: $listId (${items.size} items)"

        // Set up nested RecyclerView for items in this group
        holder.itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ItemListAdapter(items)
        }

        Log.d(TAG, "Finished binding group $listId")
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount returning ${listIdKeys.size}")
        return listIdKeys.size
    }
}

class ItemListAdapter(private val items: List<Item>) :
    RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {
    companion object {
        private const val TAG = "FetchApp"
    }

    init {
        Log.d(TAG, "ItemListAdapter initialized with ${items.size} items")
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textViewItemName)
        val idTextView: TextView = view.findViewById(R.id.textViewItemId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "ItemListAdapter.onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Log.d(TAG, "ItemListAdapter.onBindViewHolder for item ${item.id}")
        holder.nameTextView.text = item.name
        holder.idTextView.text = "ID: ${item.id}"
    }

    override fun getItemCount(): Int {
        return items.size
    }
}