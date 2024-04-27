package net.mjstudio.rnkakao.social

import android.content.ActivityNotFoundException
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.kakao.sdk.friend.client.PickerClient
import com.kakao.sdk.friend.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.model.PickerOrientation
import com.kakao.sdk.friend.model.PickerOrientation.LANDSCAPE
import com.kakao.sdk.friend.model.PickerOrientation.PORTRAIT
import com.kakao.sdk.friend.model.SelectedUsers
import com.kakao.sdk.friend.model.ViewAppearance.AUTO
import com.kakao.sdk.friend.model.ViewAppearance.DARK
import com.kakao.sdk.friend.model.ViewAppearance.LIGHT
import com.kakao.sdk.talk.TalkApiClient
import net.mjstudio.rnkakao.core.util.RNCKakaoResponseNotFoundException
import net.mjstudio.rnkakao.core.util.argArr
import net.mjstudio.rnkakao.core.util.argMap
import net.mjstudio.rnkakao.core.util.getBooleanElseNull
import net.mjstudio.rnkakao.core.util.getIntElseNull
import net.mjstudio.rnkakao.core.util.onMain
import net.mjstudio.rnkakao.core.util.pushMapList
import net.mjstudio.rnkakao.core.util.putBooleanIfNotNull
import net.mjstudio.rnkakao.core.util.putIntIfNotNull
import net.mjstudio.rnkakao.core.util.rejectWith

class RNCKakaoSocialModule internal constructor(context: ReactApplicationContext) :
  KakaoSocialSpec(context) {
    override fun getName(): String {
      return NAME
    }

    @ReactMethod
    override fun getProfile(promise: Promise) =
      onMain {
        TalkApiClient.instance.profile { profile, error ->
          if (error != null) {
            promise.rejectWith(error)
            return@profile
          }
          if (profile == null) {
            promise.rejectWith(RNCKakaoResponseNotFoundException("profile"))
            return@profile
          }
          promise.resolve(
            argMap().apply {
              putString("nickname", profile.nickname)
              putString("countryISO", profile.countryISO)
              putString("profileImageUrl", profile.profileImageUrl)
              putString("thumbnailUrl", profile.thumbnailUrl)
            },
          )
        }
      }

    @ReactMethod
    override fun selectFriends(
      multiple: Boolean,
      mode: String?,
      options: ReadableMap?,
      promise: Promise,
    ) = onMain {
      val context =
        currentActivity ?: run {
          promise.reject(ActivityNotFoundException())
          return@onMain
        }
      val callback = { users: SelectedUsers?, error: Throwable? ->
        if (error != null) {
          promise.rejectWith(error)
        } else if (users == null) {
          promise.rejectWith(RNCKakaoResponseNotFoundException("users"))
        } else {
          promise.resolve(
            argMap().apply {
              putInt("totalCount", users.totalCount)
              putArray(
                "users",
                argArr().pushMapList(
                  users.users?.map {
                    argMap().apply {
                      putString("uuid", it.uuid)
                      putIntIfNotNull("id", it.id?.toInt())
                      putBooleanIfNotNull("favorite", it.favorite)
                      putString("profileNickname", it.profileNickname)
                      putString("profileThumbnailImage", it.profileThumbnailImage)
                    }
                  } ?: listOf(),
                ),
              )
            },
          )
        }
      }

      if (!multiple) {
        if (mode == "popup") {
          PickerClient.instance.selectFriendPopup(context, getParams(options), callback)
        } else {
          PickerClient.instance.selectFriend(context, getParams(options), callback)
        }
      } else {
        if (mode == "popup") {
          PickerClient.instance.selectFriendsPopup(context, getParams(options), callback)
        } else {
          PickerClient.instance.selectFriends(context, getParams(options), callback)
        }
      }
    }

    private fun getParams(options: ReadableMap?) =
      OpenPickerFriendRequestParams(
        title = options?.getString("title"),
        viewAppearance =
          when (options?.getString("viewAppearance")) {
            "dark" -> DARK
            "light" -> LIGHT
            else -> AUTO
          },
        orientation =
          when (options?.getString("orientation")) {
            "portrait" -> PORTRAIT
            "landscape" -> LANDSCAPE
            else -> PickerOrientation.AUTO
          },
        enableSearch = options?.getBooleanElseNull("enableSearch"),
        showMyProfile = options?.getBooleanElseNull("showMyProfile"),
        showFavorite = options?.getBooleanElseNull("showFavorite"),
        showPickedFriend = options?.getBooleanElseNull("showPickedFriend"),
        maxPickableCount = options?.getIntElseNull("maxPickableCount"),
        minPickableCount = options?.getIntElseNull("minPickableCount"),
      )

    companion object {
      const val NAME = "RNCKakaoSocial"
    }
  }
