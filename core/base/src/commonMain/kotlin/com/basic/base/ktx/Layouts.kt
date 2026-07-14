package com.basic.base.ktx

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 基础居中布局组件
 * 核心逻辑：确保中间部分 [center] 绝对居中，且不与左右 [left], [right] 重叠
 */
@Composable
fun CenterTitleLayout(
    modifier: Modifier = Modifier,
    center: @Composable ((Modifier) -> Unit)? = null,
    left: @Composable ((Modifier) -> Unit)? = null,
    right: @Composable ((Modifier) -> Unit)? = null
) {
    Layout(
        modifier = modifier,
        content = {
            // 使用 contentAlignment = Alignment.Center 确保子项在 Box 内垂直居中（前提是 Box 占据了 Layout 的 full height）
            Box(contentAlignment = Alignment.Center) { left?.invoke(Modifier) }
            Box(contentAlignment = Alignment.Center) { right?.invoke(Modifier) }
            Box(contentAlignment = Alignment.Center) { center?.invoke(Modifier) }
        }
    ) { measurables, constraints ->
        val leftPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val rightPlaceable = measurables[1].measure(constraints.copy(minWidth = 0))

        val leftWidth = leftPlaceable.width
        val rightWidth = rightPlaceable.width
        // 为了实现真正的居中，两侧留出的空间必须相等（取左右最大值）
        val sideWidth = maxOf(leftWidth, rightWidth)

        val centerConstraints = constraints.copy(
            minWidth = 0,
            maxWidth = (constraints.maxWidth - sideWidth * 2).coerceAtLeast(0)
        )
        val centerPlaceable = measurables[2].measure(centerConstraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            // 由于 Box 已经占据了 full height 且内部居中，这里 place y 设为 0 即可
            leftPlaceable.placeRelative(0, 0)
            rightPlaceable.placeRelative(constraints.maxWidth - rightWidth, 0)
            centerPlaceable.placeRelative(
                (constraints.maxWidth - centerPlaceable.width) / 2,
                0
            )
        }
    }
}

/**
 * 留白
 * @param value Dp
 */
@Composable
fun WidthSpacer(value: Dp) = HorizontalDivider(modifier = Modifier.width(value), color = Color.Transparent)

@Composable
fun HeightSpacer(value: Dp) = VerticalDivider(modifier = Modifier.height(value), color = Color.Transparent)

/**
 * 文本输入框
 * @param modifier Modifier
 * @param textStyle TextStyle
 */
@Composable
fun ComposeEditText(
    contentText: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    hint: String = "",
    hintStyle: TextStyle = TextStyle(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int = Int.MAX_VALUE,
    startIcon: DrawableResource? = null,
    iconSpace: Dp = 6.dp,
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enable: Boolean = true,
    solidColor: SolidColor = SolidColor(Color(0xFF008FFF)),
    textChange: (String) -> Unit,
) {
    if (enable) {
        BasicTextField(
            value = contentText,
            onValueChange = {
                if (it.length <= maxLength) {
                    when (keyboardOptions.keyboardType) {
                        KeyboardType.Phone,
                        KeyboardType.Number -> {
                            if (it.isDigitsOnly()) {
                                textChange.invoke(it)
                            }
                        }

                        else -> {
                            textChange.invoke(it)
                        }
                    }
                }
            },
            modifier = modifier,
            singleLine = singleLine,
            textStyle = textStyle,
            decorationBox = { innerTextField ->
                Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                    startIcon?.let {
                        Image(painter = painterResource(startIcon), contentDescription = null)
                        Spacer(modifier = Modifier.width(iconSpace))
                    }
                    Box(
                        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                        modifier = Modifier.fillMaxWidth(),
                        propagateMinConstraints = true
                    ) {
                        if (contentText.isEmpty()) {
                            Text(text = hint, style = hintStyle)
                        }
                        innerTextField()
                    }
                }
            },
            cursorBrush = solidColor,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            visualTransformation = visualTransformation
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
            Text(text = contentText, style = textStyle)
        }
    }
}

/**
 * 文本输入框
 * @param modifier Modifier
 * @param textStyle TextStyle
 */
@Composable
fun ComposeEditText(
    contentText: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    hint: AnnotatedString,
    hintStyle: TextStyle = TextStyle(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int = Int.MAX_VALUE,
    startIcon: DrawableResource? = null,
    iconSpace: Dp = 6.dp,
    singleLine: Boolean = true,
    solidColor: SolidColor = SolidColor(Color(0xFF008FFF)),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textChange: (String) -> Unit,
) {
    BasicTextField(
        value = contentText,
        onValueChange = {
            if (it.length <= maxLength) {
                when (keyboardOptions.keyboardType) {
                    KeyboardType.Phone,
                    KeyboardType.Number -> {
                        if (it.isDigitsOnly()) {
                            textChange.invoke(it)
                        }
                    }

                    else -> {
                        textChange.invoke(it)
                    }
                }
            }
        },
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        decorationBox = { innerTextField ->
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                startIcon?.let {
                    Image(painter = painterResource(startIcon), contentDescription = null)
                    Spacer(modifier = Modifier.width(iconSpace))
                }
                Box(
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth(),
                    propagateMinConstraints = true
                ) {
                    if (contentText.isEmpty()) {
                        Text(text = hint, style = hintStyle)
                    }
                    innerTextField()
                }
            }
        },
        cursorBrush = solidColor,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation
    )
}

/**
 * 文本输入框
 * @param modifier Modifier
 * @param textStyle TextStyle
 */
@Composable
fun ComposeEditText(
    contentText: AnnotatedString,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    hint: AnnotatedString,
    hintStyle: TextStyle = TextStyle(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int = Int.MAX_VALUE,
    startIcon: DrawableResource? = null,
    iconSpace: Dp = 6.dp,
    singleLine: Boolean = true,
    solidColor: SolidColor = SolidColor(Color(0xFF008FFF)),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textChange: (String) -> Unit,
) {
    BasicTextField(
        value = TextFieldValue(contentText.text),
        onValueChange = {
            if (it.text.length <= maxLength) {
                when (keyboardOptions.keyboardType) {
                    KeyboardType.Phone,
                    KeyboardType.Number -> {
                        if (it.text.isDigitsOnly()) {
                            textChange.invoke(it.text)
                        }
                    }

                    else -> {
                        textChange.invoke(it.text)
                    }
                }
            }
        },
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        decorationBox = { innerTextField ->
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                startIcon?.let {
                    Image(painter = painterResource(startIcon), contentDescription = null)
                    Spacer(modifier = Modifier.width(iconSpace))
                }
                Box(
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth(),
                    propagateMinConstraints = true
                ) {
                    if (contentText.isEmpty()) {
                        Text(text = hint, style = hintStyle)
                    }
                    innerTextField()
                }
            }
        },
        cursorBrush = solidColor,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation
    )
}


/**
 * 添加可点击的文字
 * @receiver AnnotatedString.Builder
 * @param text String
 * @param style SpanStyle
 * @param click Function0<Unit>
 */
fun AnnotatedString.Builder.appendLinkText(text: String, style: SpanStyle = SpanStyle(color = Color(0xFF008FFF)), click: () -> Unit) {
    append(buildAnnotatedString {
        append(text)
        addLink(
            LinkAnnotation.Clickable(text, TextLinkStyles(style = style)) {
                click.invoke()
            }, 0, text.length
        )
    })
}

/**
 * 构建稳定的可点击富文本，避免直接依赖 Text + LinkAnnotation 的点击命中。
 *
 * @param modifier 外层布局修饰符。
 * @param style 文本整体样式。
 * @param content 富文本构建内容。
 */
@Composable
fun LinkText(
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(),
    content: LinkTextBuilder.() -> Unit
) {
    val builder = LinkTextBuilder()
    builder.content()
    val linkText = builder.build()
    ClickableText(
        text = linkText.text,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            linkText.actions.forEach { (tag, action) ->
                if (linkText.text.getStringAnnotations(tag = tag, start = offset, end = offset).isNotEmpty()) {
                    action()
                    return@ClickableText
                }
            }
        }
    )
}

/**
 * 可点击富文本构建器。
 */
class LinkTextBuilder internal constructor() {
    private val builder = AnnotatedString.Builder()
    private val actions = linkedMapOf<String, () -> Unit>()

    /**
     * 追加普通文本。
     *
     * @param text 普通文本内容。
     */
    fun append(text: String) {
        builder.append(text)
    }

    /**
     * 追加可点击文本。
     *
     * @param text 可点击文本内容。
     * @param style 可点击文本样式。
     * @param click 点击回调。
     */
    fun appendLinkText(
        text: String,
        style: SpanStyle = SpanStyle(color = Color(0xFF008FFF)),
        click: () -> Unit
    ) {
        val tag = "link_${actions.size}"
        actions[tag] = click
        builder.pushStringAnnotation(tag = tag, annotation = tag)
        builder.pushStyle(style)
        builder.append(text)
        builder.pop()
        builder.pop()
    }

    internal fun build(): LinkTextValue {
        return LinkTextValue(builder.toAnnotatedString(), actions.toMap())
    }
}

/**
 * 可点击富文本内容和事件集合。
 *
 * @param text 富文本内容。
 * @param actions 可点击区域事件集合。
 */
class LinkTextValue internal constructor(
    val text: AnnotatedString,
    val actions: Map<String, () -> Unit>
)

private fun String.isDigitsOnly(): Boolean {
    for (char in this) {
        if (!char.isDigit()) {
            return false
        }
    }
    return true
}
