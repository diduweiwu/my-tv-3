# 赞赏弹窗并排展示微信/支付宝收款码

## 目标
点击"赞赏作者"后，弹窗中并排展示 `wechat.png` 和 `alipay.png` 两张收款码图片。

## 现状分析
- 当前弹窗 `modal.xml` 使用 ConstraintLayout，只包含一个 `AppCompatImageView`
- `ModalFragment.kt` 通过 Glide 加载单个图片资源
- `SettingFragment.kt` 点击赞赏按钮时传递 `R.drawable.appreciate` 给 ModalFragment
- 已存在 `wechat.png` 和 `alipay.png` 两张图片在 `drawable` 目录中

## 实施方案

### 1. 修改 `modal.xml` 布局
将 ConstraintLayout 改为垂直 LinearLayout，内部嵌套一个水平 LinearLayout 用于并排展示两张图片：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/modal"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center">

    <!-- 并排展示两张收款码 -->
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/modal_image_left"
            android:layout_width="150dp"
            android:layout_height="150dp"
            android:layout_margin="8dp"
            android:background="@color/white"
            android:contentDescription="WeChat Pay" />

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/modal_image_right"
            android:layout_width="150dp"
            android:layout_height="150dp"
            android:layout_margin="8dp"
            android:background="@color/white"
            android:contentDescription="Alipay" />
    </LinearLayout>

    <androidx.appcompat.widget.AppCompatTextView
        android:id="@+id/modal_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:background="@color/blur"
        android:paddingHorizontal="10dp"
        android:maxLines="1"
        android:ellipsize="end"
        android:textColor="@color/title_blur"
        android:textSize="14sp"
        tools:text=""/>

</LinearLayout>
```

### 2. 修改 `ModalFragment.kt`
在 `onViewCreated` 方法中，当 `KEY_DRAWABLE_ID` 为赞赏类型时，同时加载两张图片：

```kotlin
// 在 onViewCreated 方法中
val drawableId = arguments?.getInt(KEY_DRAWABLE_ID)
if (drawableId == R.drawable.appreciate) {
    // 赞赏弹窗：并排展示两张收款码
    Glide.with(requireContext())
        .load(R.drawable.wechat)
        .into(binding.modalImageLeft)
    Glide.with(requireContext())
        .load(R.drawable.alipay)
        .into(binding.modalImageRight)
    binding.modalImageLeft.visibility = View.VISIBLE
    binding.modalImageRight.visibility = View.VISIBLE
    // 隐藏原来的单张图片
    binding.modalImage.visibility = View.GONE
    binding.modalText.visibility = View.GONE
} else if (drawableId != null) {
    Glide.with(requireContext())
        .load(drawableId)
        .into(binding.modalImage)
    binding.modalImage.visibility = View.VISIBLE
    binding.modalImageLeft.visibility = View.GONE
    binding.modalImageRight.visibility = View.GONE
    binding.modalText.visibility = View.GONE
}
```

同时保留 URL 二维码的逻辑。

### 3. 文件修改清单
- 修改 `/Users/itest/Code/my-tv-0/app/src/main/res/layout/modal.xml`
- 修改 `/Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ModalFragment.kt`

## 风险评估
- 低风险：仅修改 UI 布局，不影响核心功能
- 兼容：保留原有二维码弹窗功能，只在赞赏场景使用双图
