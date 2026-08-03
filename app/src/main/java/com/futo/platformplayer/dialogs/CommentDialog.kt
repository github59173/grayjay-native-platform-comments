package com.futo.platformplayer.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.futo.platformplayer.R
import com.futo.platformplayer.UIDialogs
import com.futo.platformplayer.api.media.IPlatformClient
import com.futo.platformplayer.api.media.PlatformID
import com.futo.platformplayer.api.media.models.PlatformAuthorLink
import com.futo.platformplayer.api.media.models.comments.CommentDestination
import com.futo.platformplayer.api.media.models.comments.CommentDestinationSelection
import com.futo.platformplayer.api.media.models.comments.CommentSubmissionGuard
import com.futo.platformplayer.api.media.models.comments.IPlatformComment
import com.futo.platformplayer.api.media.models.comments.PlatformCommentCapability
import com.futo.platformplayer.api.media.models.comments.PlatformCommentMutationResult
import com.futo.platformplayer.api.media.models.comments.PolycentricPlatformComment
import com.futo.platformplayer.api.media.models.ratings.RatingLikeDislikes
import com.futo.platformplayer.constructs.Event1
import com.futo.platformplayer.dp
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.selectBestImage
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StatePlatform
import com.futo.platformplayer.states.StatePolycentric
import com.futo.polycentric.core.ClaimType
import com.futo.polycentric.core.Store
import com.futo.polycentric.core.SystemState
import com.futo.polycentric.core.fullyBackfillServersAnnounceExceptions
import com.futo.polycentric.core.systemToURLInfoSystemLinkUrl
import com.futo.polycentric.core.toURLInfoSystemLinkUrl
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import userpackage.Protocol
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.time.OffsetDateTime

internal data class CommentDialogMode(
    val isEditing: Boolean,
    val initialText: String
)

internal fun resolveCommentDialogMode(
    editTarget: IPlatformComment?,
    initialText: String = ""
): CommentDialogMode =
    CommentDialogMode(
        isEditing = editTarget != null,
        initialText = editTarget?.message ?: initialText
    )

/** Returns the exact text prefix used for a YouTube-style directed reply. */
internal fun resolveReplyMention(authorName: String, authorUrl: String?): String {
    val urlHandle = authorUrl
        ?.substringBefore('#')
        ?.substringBefore('?')
        ?.split('/')
        ?.asSequence()
        ?.mapNotNull { segment ->
            runCatching {
                URLDecoder.decode(segment, StandardCharsets.UTF_8.name())
            }.getOrNull()
        }
        ?.firstOrNull { it.startsWith('@') && it.length > 1 }
    val nameHandle = authorName.trim().takeIf { it.startsWith('@') && it.length > 1 }
    val handle = (urlHandle ?: nameHandle)
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() && it.none(Char::isWhitespace) }
        ?: return ""
    return "@$handle "
}

class CommentDialog(
    context: Context?,
    private val contextUrl: String,
    private val ref: Protocol.Reference?,
    private val platformClient: IPlatformClient? = null,
    private val parentPlatformComment: IPlatformComment? = null,
    private val editTarget: IPlatformComment? = null,
    private val initialText: String = "",
    private val preferredDestination: CommentDestination? = null,
    private val restrictToPreferredDestination: Boolean = false
) : AlertDialog(context) {
    private lateinit var buttonCreate: LinearLayout
    private lateinit var buttonCreateText: TextView
    private lateinit var buttonCancel: MaterialButton
    private lateinit var editComment: EditText
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var textCharacterCount: TextView
    private lateinit var textCharacterCountMax: TextView
    private lateinit var spinnerDestination: Spinner
    private lateinit var textIdentity: TextView

    private val destinations = mutableListOf<CommentDestination>()
    private val submissionGuard = CommentSubmissionGuard()
    private var submitting = false
    private var platformIdentity: String? = null
    private var polycentricIdentity: String? = null

    val onCommentAdded = Event1<IPlatformComment>()
    val onCommentUpdated = Event1<IPlatformComment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_comment, null))

        buttonCancel = findViewById(R.id.button_cancel)
        buttonCreate = findViewById(R.id.button_create)
        buttonCreateText = findViewById(R.id.button_create_text)
        editComment = findViewById(R.id.edit_comment)
        textCharacterCount = findViewById(R.id.character_count)
        textCharacterCountMax = findViewById(R.id.character_count_max)
        spinnerDestination = findViewById(R.id.spinner_destination)
        textIdentity = findViewById(R.id.text_commenting_identity)

        configureDestinations()
        if (destinations.isEmpty()) {
            UIDialogs.toast(context, context.getString(R.string.no_comment_destination_available))
            dismiss()
            return
        }

        val mode = resolveCommentDialogMode(editTarget, initialText)
        if (mode.initialText.isNotEmpty()) {
            editComment.setText(mode.initialText)
            editComment.setSelection(editComment.text.length)
        }
        if (mode.isEditing) {
            buttonCreateText.setText(R.string.edit_comment)
        }

        setCanceledOnTouchOutside(false)
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleCloseAttempt()
                true
            } else false
        }

        editComment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateCharacterCount()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        buttonCancel.setOnClickListener { handleCloseAttempt() }
        setOnCancelListener { handleCloseAttempt() }
        buttonCreate.setOnClickListener { submit() }

        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        focus()
        updateCharacterCount()
        loadIdentities()
    }

    private fun configureDestinations() {
        val client = platformClient ?: StatePlatform.instance.getContentClientOrNull(contextUrl)
        val platformAvailable = when {
            editTarget != null -> PlatformCommentCapability.COMMENTS_EDIT in editTarget.capabilities
            parentPlatformComment != null -> PlatformCommentCapability.COMMENTS_REPLY in parentPlatformComment.capabilities
            else -> client?.capabilities?.hasCommentsCreate == true
        }
        val polycentricAvailable = editTarget == null && parentPlatformComment == null &&
            ref != null && StatePolycentric.instance.processHandle != null

        if (platformAvailable) destinations.add(CommentDestination.PLATFORM)
        if (polycentricAvailable) destinations.add(CommentDestination.POLYCENTRIC)

        val availableDestinations = CommentDestinationSelection.restrictAvailable(
            destinations,
            preferredDestination,
            restrictToPreferredDestination
        )
        destinations.clear()
        destinations.addAll(availableDestinations)

        val labels = destinations.map {
            when (it) {
                CommentDestination.PLATFORM -> client?.name ?: context.getString(R.string.platform)
                CommentDestination.POLYCENTRIC -> context.getString(R.string.polycentric)
            }
        }
        spinnerDestination.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, labels)
        spinnerDestination.visibility = if (destinations.size > 1) View.VISIBLE else View.GONE

        val preferenceKey = "comment_destination_${client?.id ?: "default"}"
        val preferences = context.getSharedPreferences("comment_destinations", Context.MODE_PRIVATE)
        val remembered = preferences.getString(preferenceKey, null)
            ?.let { runCatching { CommentDestination.valueOf(it) }.getOrNull() }
        val initialDestination = CommentDestinationSelection.resolve(destinations, preferredDestination, remembered)
        val initialIndex = destinations.indexOf(initialDestination)
        if (initialIndex >= 0) spinnerDestination.setSelection(initialIndex)
        spinnerDestination.setOnItemSelectedListener(SimpleItemSelectedListener {
            preferences.edit().putString(preferenceKey, selectedDestination().name).apply()
            updateCharacterCount()
            updateIdentity()
        })
        updateIdentity()
    }

    private fun loadIdentities() {
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            val client = platformClient ?: StatePlatform.instance.getContentClientOrNull(contextUrl)
            val resolvedPlatformIdentity = if (CommentDestination.PLATFORM in destinations)
                StatePlatform.instance.getCommentingIdentity(contextUrl)
            else null
            val resolvedPolycentricIdentity = if (CommentDestination.POLYCENTRIC in destinations)
                StatePolycentric.instance.processHandle?.let { processHandle ->
                    SystemState.fromStorageTypeSystemState(
                        Store.instance.getSystemState(processHandle.system)
                    ).username
                }
            else null
            withContext(Dispatchers.Main) {
                platformIdentity = resolvedPlatformIdentity ?: client?.name ?: context.getString(R.string.platform)
                polycentricIdentity = resolvedPolycentricIdentity ?: context.getString(R.string.polycentric)
                updateIdentity()
            }
        }
    }

    private fun updateIdentity() {
        if (!::textIdentity.isInitialized || destinations.isEmpty()) return
        val identity = when (selectedDestination()) {
            CommentDestination.PLATFORM -> platformIdentity ?: platformClient?.name ?: context.getString(R.string.platform)
            CommentDestination.POLYCENTRIC -> polycentricIdentity ?: context.getString(R.string.polycentric)
        }
        textIdentity.visibility = View.VISIBLE
        textIdentity.text = context.getString(R.string.commenting_as, identity)
    }

    private fun selectedDestination(): CommentDestination =
        destinations.getOrElse(spinnerDestination.selectedItemPosition.coerceAtLeast(0)) { destinations.first() }

    private fun maxLength(): Int = if (selectedDestination() == CommentDestination.PLATFORM) 10000
        else PolycentricPlatformComment.MAX_COMMENT_SIZE

    private fun updateCharacterCount() {
        if (!::editComment.isInitialized) return
        val count = editComment.text?.length ?: 0
        val max = maxLength()
        textCharacterCount.text = NumberFormat.getIntegerInstance().format(count)
        textCharacterCountMax.text = context.getString(R.string.comment_character_count_max, max)
        val invalid = count > max || count == 0
        textCharacterCount.setTextColor(if (count > max) Color.RED else Color.WHITE)
        textCharacterCountMax.setTextColor(if (count > max) Color.RED else Color.WHITE)
        buttonCreate.alpha = if (invalid || submitting) 0.4f else 1.0f
    }

    private fun submit() {
        if (submitting) return
        val message = editComment.text.toString()
        if (message.isBlank()) {
            UIDialogs.toast(context, context.getString(R.string.comment_should_not_be_blank))
            return
        }
        if (message.length > maxLength()) {
            UIDialogs.toast(context, context.getString(R.string.comment_is_too_long))
            return
        }
        val key = listOf(contextUrl, parentPlatformComment?.stableId, editTarget?.stableId, message).joinToString("|")
        if (!submissionGuard.tryAcquire(key)) return

        submitting = true
        buttonCreate.isEnabled = false
        clearFocus()
        updateCharacterCount()
        val destination = selectedDestination()

        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            val result = when (destination) {
                CommentDestination.PLATFORM -> when {
                    editTarget != null -> StatePlatform.instance.editComment(editTarget, message)
                    parentPlatformComment != null -> StatePlatform.instance.replyToComment(parentPlatformComment, message)
                    else -> StatePlatform.instance.createComment(contextUrl, message)
                }
                CommentDestination.POLYCENTRIC -> try {
                    PlatformCommentMutationResult(success = true, comment = createPolycentricComment(message))
                } catch (error: Throwable) {
                    Logger.w(TAG, "Failed to post Polycentric comment", error)
                    PlatformCommentMutationResult(success = false, message = error.message)
                }
            }

            submissionGuard.release(key)
            withContext(Dispatchers.Main) {
                result.comment?.let {
                    if (editTarget != null) onCommentUpdated.emit(it) else onCommentAdded.emit(it)
                }
                if (result.success) {
                    dismiss()
                } else {
                    UIDialogs.toast(
                        context,
                        result.message ?: context.getString(R.string.failed_to_post_comment),
                        true
                    )
                    submitting = false
                    buttonCreate.isEnabled = true
                    updateCharacterCount()
                }
            }
        }
    }

    private fun createPolycentricComment(message: String): PolycentricPlatformComment {
        val nonNullRef = ref ?: throw IllegalStateException(context.getString(R.string.polycentric_parent_unavailable))
        val processHandle = StatePolycentric.instance.processHandle
            ?: throw IllegalStateException(context.getString(R.string.please_login_to_post_a_comment))
        val eventPointer = processHandle.post(message, nonNullRef)
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            try { processHandle.fullyBackfillServersAnnounceExceptions() }
            catch (error: Throwable) { Logger.e(TAG, "Failed to backfill Polycentric comment", error) }
        }
        val systemState = SystemState.fromStorageTypeSystemState(Store.instance.getSystemState(processHandle.system))
        val dp25 = 25.dp(context.resources)
        return PolycentricPlatformComment(
            contextUrl = contextUrl,
            author = PlatformAuthorLink(
                id = PlatformID("polycentric", processHandle.system.systemToURLInfoSystemLinkUrl(systemState.servers.toList()), null, ClaimType.POLYCENTRIC.value.toInt()),
                name = systemState.username,
                url = processHandle.system.systemToURLInfoSystemLinkUrl(systemState.servers.toList()),
                thumbnail = systemState.avatar.selectBestImage(dp25 * dp25)?.toURLInfoSystemLinkUrl(processHandle, systemState.servers.toList()),
                subscribers = null
            ),
            msg = message,
            rating = RatingLikeDislikes(0, 0),
            date = OffsetDateTime.now(),
            eventPointer = eventPointer,
            parentReference = nonNullRef
        )
    }

    private fun handleCloseAttempt() {
        if (submitting) return
        val originalText = editTarget?.message ?: initialText
        if (editComment.text.isEmpty() || originalText == editComment.text.toString()) {
            clearFocus()
            dismiss()
        } else {
            UIDialogs.showConfirmationDialog(context, context.getString(R.string.not_empty_close), action = {
                clearFocus()
                dismiss()
            })
        }
    }

    private fun focus() {
        editComment.requestFocus()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun clearFocus() {
        editComment.clearFocus()
        if (::inputMethodManager.isInitialized)
            currentFocus?.let { inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    companion object {
        private const val TAG = "CommentDialog"
    }
}

private class SimpleItemSelectedListener(private val selected: () -> Unit) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = selected()
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
