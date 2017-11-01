package name.dmx.readhubclient.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hzzh.baselibrary.net.transformer.SchedulerTransformer
import kotlinx.android.synthetic.main.topic_fragment.*
import name.dmx.readhubclient.R
import name.dmx.readhubclient.WebViewActivity
import name.dmx.readhubclient.adapter.TopicListAdapter
import name.dmx.readhubclient.http.DataRepository
import name.dmx.readhubclient.model.Topic

/**
 * Created by dmx on 17-10-31.
 */
class TopicFragment : Fragment() {
    private var lastCursor: Long? = null
    private val PAGE_SIZE = 10
    private val dataList: MutableList<Topic> = MutableList(0, { _ ->
        Topic()
    })
    private val onItemClickListener = object : TopicListAdapter.OnItemClickListener {
        override fun onItemClick(view: View, position: Int) {
            val item = dataList[position]
            val intent = WebViewActivity.makeIntent(context, "https://readhub.me/topic/${item.id}", item.title, "")
            startActivity(intent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater?.inflate(R.layout.topic_fragment, container, false)
        return view!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        getData()
        smartRefreshLayout.setOnRefreshListener {
            lastCursor = null
            getData()
        }
        smartRefreshLayout.setOnLoadmoreListener {
            getData()
        }
    }

    private fun getData() {
        val observable = DataRepository.getService(context).getTopics(lastCursor, PAGE_SIZE).compose(SchedulerTransformer())
        observable.subscribe({ data ->
            if (lastCursor == null) {
                dataList.clear()
            }
            dataList.addAll(dataList.size, data.data?.toList()!!)
            val adapter = TopicListAdapter(context, dataList)
            adapter.onItemClickListener = onItemClickListener
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            lastCursor = data.data?.last()?.order
            smartRefreshLayout.finishLoadmore()
            smartRefreshLayout.finishRefresh()
            adapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(dataList.size - PAGE_SIZE)
        }, { error ->
            error.printStackTrace()
            smartRefreshLayout.finishLoadmore()
            smartRefreshLayout.finishRefresh()
        })
    }

    companion object {
        fun newInstance(): TopicFragment {
            val topicFragment = TopicFragment()
            return topicFragment
        }
    }
}