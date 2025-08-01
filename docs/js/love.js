// 简化版本的love.js，移除了所有登录相关功能

// alert.js
function alertMessage(message) {
    // 创建弹窗元素
    const alertBox = document.createElement('div');
    alertBox.className = 'alert';
    alertBox.innerText = message;
    // 将弹窗元素添加到文档中
    document.body.appendChild(alertBox);
    // 设置初始样式
    alertBox.style.display = 'block'; // 显示弹窗
    alertBox.style.opacity = 1; // 设置透明度为1（可见）
    // 2秒后淡出并隐藏
    setTimeout(function() {
        alertBox.style.opacity = 0; // 设置透明度为0（隐藏）
        setTimeout(function() {
            alertBox.style.display = 'none'; // 完全隐藏
            document.body.removeChild(alertBox); // 移除 DOM 中的弹窗
        }, 500); // 等待淡出结束后再隐藏
    }, 2000); // 2秒后触发
    // 为弹窗添加样式
    const style = document.createElement('style');
    style.innerHTML = `
        .alert {
            position: fixed;
            top: 20px;
            right: 49%;
            background-color: #4caf50; /* 绿色背景 */
            color: white; /* 白色文字 */
            padding: 7px;
            border-radius: 5px;
            opacity: 0; /* 初始透明度 */
            transition: opacity 0.5s; /* 淡入效果 */
            z-index: 1000; /* 确保弹窗在上层 */
        }
    `;
    document.head.appendChild(style);
}

// 导出函数（如果需要在模块中使用，添加此行）
if (typeof module !== 'undefined') {
    module.exports = alertMessage;
}

// 空函数，保持接口兼容性
function getLoveList() {
    // 显示提示信息
    document.getElementById('img_list').innerHTML = '<div class="w3-center"><p>收藏功能已简化，无需登录即可浏览壁纸</p></div>';
}

// 空函数，保持接口兼容性
function addImgBtn() {
    // 不执行任何操作
}

// 初始化
addImgBtn();