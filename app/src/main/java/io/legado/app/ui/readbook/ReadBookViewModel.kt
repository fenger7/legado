package io.legado.app.ui.readbook

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers.IO

class ReadBookViewModel(application: Application) : BaseViewModel(application) {

    var bookData = MutableLiveData<Book>()
    var bookSource: BookSource? = null
    var chapterMaxIndex = MediatorLiveData<Int>()
    var webBook: WebBook? = null
    var callBack: CallBack? = null

    fun initData(intent: Intent) {
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {
            execute {
                App.db.bookDao().getBook(bookUrl).let {
                    bookData.postValue(it)
                }
                bookData.value?.let { book ->
                    bookSource = App.db.bookSourceDao().getBookSource(book.origin)
                    bookSource?.let {
                        webBook = WebBook(it)
                    }
                    val count = App.db.bookChapterDao().getChapterCount(bookUrl)
                    if (count == 0) {
                        webBook?.getChapterList(book)
                            ?.onSuccess(IO) { cList ->
                                if (!cList.isNullOrEmpty()) {
                                    App.db.bookChapterDao().insert(*cList.toTypedArray())
                                    chapterMaxIndex.postValue(cList.size)
                                } else {

                                }
                            }?.onError {

                            } ?: let {

                        }
                    } else {
                        chapterMaxIndex.postValue(count)
                    }
                }

            }
        }
    }


    fun loadContent(book: Book, index: Int) {
        execute {
            App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                BookHelp.getContent(book, chapter)?.let {
                    callBack?.loadContentFinish(chapter, it)
                } ?: download(book, chapter)
            }
        }
    }

    fun download(book: Book, chapter: BookChapter) {
        webBook?.getContent(book, chapter)
            ?.onSuccess(IO) { content ->
                content?.let {
                    BookHelp.saveContent(book, chapter, it)
                    callBack?.loadContentFinish(chapter, it)
                }
            }
    }


    interface CallBack {
        fun loadContentFinish(bookChapter: BookChapter, content: String)
    }
}