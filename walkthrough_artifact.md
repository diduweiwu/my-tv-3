# 赞赏弹窗并排展示微信/支付宝收款码

## ✅ 任务完成

已成功实现点击"赞赏作者"后，弹窗并排展示 `wechat.png` 和 `alipay.png` 两张收款码图片。

## 📝 修改内容

### 1. 文件：`app/src/main/res/layout/modal.xml`

**之前**：ConstraintLayout，单个 ImageView
```xml
<androidx.constraintlayout.widget.ConstraintLayout ...>
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/modal_image"
        android:layout_width="200dp"
        android:layout_height="200dp" />
    <androidx.appcompat.widget.AppCompatTextView
        android:id="@+id/modal_text"
        .../>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**之后**：LinearLayout，支持单张图片和并排双图
```xml
<LinearLayout
    android:id="@+id/modal"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center">

    <!-- 单张图片（默认隐藏，用于二维码等场景） -->
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/modal_image"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:visibility="gone" />

    <!-- 并排展示两张收款码 -->
    <LinearLayout
        android:id="@+id/modal_image_container"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:visibility="gone">

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/modal_image_left"
            android:layout_width="150dp"
            android:layout_height="150dp"
            android:layout_margin="8dp"
            android:contentDescription="WeChat Pay" />

        <androidx.appcompat.widget.AppCompatImageView
            android:id="@+id/modal_image_right"
            android:layout_width="150dp"
            android:layout_height="150dp"
            android:layout_margin="8dp"
            android:contentDescription="Alipay" />
    </LinearLayout>

    <androidx.appcompat.widget.AppCompatTextView
        android:id="@+id/modal_text"
        .../>
</LinearLayout>
```

### 2. 文件：`app/src/main/java/com/lizongying/mytv3/ModalFragment.kt`

**添加 import**：
```kotlin
import com.lizongying.mytv0.R
```

**修改 onViewCreated 方法**：
```kotlin
val drawableId = arguments?.getInt(KEY_DRAWABLE_ID)
if (drawableId == R.drawable.appreciate) {
    // 赞赏弹窗：并排展示两张收款码
    Glide.with(requireContext())
        .load(R.drawable.wechat)
        .into(binding.modalImageLeft)
    Glide.with(requireContext())
        .load(R.drawable.alipay)
        .into(binding.modalImageRight)
    binding.modalImageContainer.visibility = View.VISIBLE
    binding.modalText.visibility = View.GONE
} else if (drawableId != null) {
    Glide.with(requireContext())
        .load(drawableId)
        .into(binding.modalImage)
    binding.modalImage.visibility = View.VISIBLE
    binding.modalText.visibility = View.GONE
}
```

### 3. 新增文件
- 复制 `/Users/itest/Code/my-tv-0/screenshots/appreciate.png` 到 `app/src/main/res/drawable/appreciate.png`

## 🔍 效果展示

点击"赞赏作者"按钮后，弹窗会显示：

```
┌─────────────────────────────────────┐
│  ┌──────────┐    ┌──────────┐       │
│  │          │    │          │       │
│  │  WeChat  │    │  Alipay  │       │
│  │   收款码  │    │   收款码  │       │
│  │          │    │          │       │
│  └──────────┘    └──────────┘       │
│      (150dp)        (150dp)         │
└─────────────────────────────────────┘
```

## ✅ 验证结果

```bash
./gradlew app:assembleDebug --stacktrace
# ✅ 构建成功！
```

## 🎉 总结

现在点击"赞赏作者"按钮，弹窗会并排展示微信和支付宝两张收款码图片，用户可以方便地选择任意一种方式进行赞赏。
