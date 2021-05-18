
import os, sys, time

from uiautomator import Device
from flask import Flask, request

app = Flask(__name__)


def pushFile(dId, fileName, filePath):
    print(dId)
    print(fileName)
    print(filePath)
    os.system('adb -s %s push %s /sdcard/DCIM/Camera/ & adb -s %s shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/DCIM/Camera/%s'%(dId, filePath, dId, fileName))

def delFiles(dId, fileList):
    for i in fileList:
        os.system('adb -s %s shell rm /sdcard/DCIM/Camera/%s & adb -s %s shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///storage/sdcard/DCIM/Camera/%s'%(dId, i, dId, i))

def delFile(dId, file):
    os.system('adb -s %s shell rm /sdcard/DCIM/Camera/%s & adb -s %s shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///storage/sdcard/DCIM/Camera/%s'%(dId, file, dId, file))

def waitAndClick(d, resId):
    if d(resourceId = resId).wait.exists(timeout = 1000):
        d(resourceId = resId).click()

def waitAndClickText(d, theText):
    if d(text = theText).wait.exists(timeout = 1000):
        d(text = theText).click()

def openApp(d, dId):
    os.system('adb -s %s shell am start -n com.xingin.xhs/.activity.SplashActivity'%dId)
    waitAndClick(d, 'com.xingin.xhs:id/ao9') # 关闭升级

@app.route('/getDevicesList')
def getDevicesList():
    f = os.popen(r'adb devices','r')
    d = f.readline()
    output = str(d)
    while d :
        d = f.readline() 
        output += str(d)
    f.close()
    return d

@app.route('/postmes')
def postMes():
    '''
    入参：
        device：设备ID
        path：绝对路径，该路径下存放文案和图片，其中文案存放在txt文件中
    例：
        /postmes?device=fa16c0f&path=D:\\xiaohongshu
    '''
    deviceId = request.args.get('device', '')
    path = request.args.get('path', '')
    d = Device(deviceId)
    fileList = []
    title = ''
    context = ''
    for file in os.listdir(path):
        filePath = os.path.join(path, file)
        if os.path.splitext(filePath)[1]=='.txt':
            with open(filePath, encoding='utf8') as f:
                allLines = f.readlines()
                title = allLines[0]
                context = ''.join(allLines[1:])
            continue
        if not os.path.isdir(filePath):
            pushFile(deviceId, file, filePath)
            fileList.append(file)
    d.wakeup()
    openApp(d, deviceId)
    waitAndClick(d, 'com.xingin.xhs:id/bo5')
    d(resourceId = 'com.xingin.xhs:id/bq8').wait.exists(timeout = 10000)
    for i in range(len(fileList)):
        d(resourceId = 'com.xingin.xhs:id/bmu')[i].click()
    waitAndClickText(d, '下一步(%s)'%(len(fileList)))
    # waitAndClick(d, 'com.xingin.xhs:id/wg')
    waitAndClick(d, 'com.xingin.xhs:id/aeu')
    d(text = '发布笔记').wait.exists(timeout = 10000)
    if title:
        d(resourceId = 'com.xingin.xhs:id/atm').set_text(title)
    if context:
        d(resourceId = 'com.xingin.xhs:id/ase').set_text(context)
    waitAndClickText(d, '发布笔记')
    # waitAndClick(d, 'com.xingin.xhs:id/bnb')
    if d(resourceId = 'com.xingin.xhs:id/drw').info['selected']:
        d(resourceId = 'com.xingin.xhs:id/drw').click()
    waitAndClick(d, 'com.xingin.xhs:id/bp9')
    # waitAndClick(d, 'com.xingin.xhs:id/a2i')
    waitAndClickText(d, '发布笔记')
    d(text = '保存到相册').wait.exists(timeout = 10000)
    os.system('adb -s %s shell am force-stop com.xingin.xhs'%(deviceId))
    delFiles(deviceId, fileList)
    return {'status': 1}

@app.route('/checkLogin')
def checkLogin():
    '''
    入参：
        device：设备ID
    出参：
        loginstatus：登录状态，True（登录） | False（未登录）
    例：
        /getuserid?device=fa16c0f
    '''
    deviceId = request.args.get('device', '')
    d = Device(deviceId)
    openApp(d, deviceId)
    os.system('adb -s %s shell am force-stop com.xingin.xhs'%(deviceId))
    return {'loginstatus': not d(resourceId = 'com.xingin.xhs:id/cik').wait.exists(timeout = 1000)}

@app.route('/getuserid')
def getUserId():
    '''
    入参：
        device：设备ID
    出参：
        XHSID：ID（字符串）
        loginstatus：登录状态，True（登录） | False（未登录）
    例：
        /getuserid?device=fa16c0f
    '''
    deviceId = request.args.get('device', '')
    d = Device(deviceId)
    openApp(d, deviceId)
    if (d(resourceId = 'com.xingin.xhs:id/cik').wait.exists(timeout = 1000)):
        return {'XHSID': '', 'loginstatus': False}
    waitAndClick(d, 'com.xingin.xhs:id/bo_')
    theText = d(resourceId = 'com.xingin.xhs:id/dcf').text
    os.system('adb -s %s shell am force-stop com.xingin.xhs'%(deviceId))
    return {'XHSID': theText[theText.index('：') + 1:], 'loginstatus': True}

@app.route('/changeInfo')
def changeInfo():
    '''
    入参：
        device：设备ID
        userName：新的用户名 可为空，为空则不修改
        userImag：新的头像路径（绝对路径） 可为空，为空则不修改
        userPasw：新密码 可为空，为空则不修改
        userOlPs：新密码不为空则旧密码也不可为空
    出参：
        changeName：昵称是否修改成功，True（成功） | False（失败）
        changeImag：头像是否修改成功，True（成功） | False（失败）
        changePassword：密码是否修改成功，True（成功） | False（失败）
    例：
        /changeInfo?device=fa16c0f&userImag=D:\\1.jpg&userName=呵呵123&userPasw=QQ123456&userOlPs=QQ1234567
    '''
    deviceId = request.args.get('device', '')
    userName = request.args.get('userName', '')
    userImag = request.args.get('userImag', '')
    userPasw = request.args.get('userPasw', '')
    userOlPs = request.args.get('userOlPs', '')

    changePassFlag = True
    changeNameFlag = True
    changeImagFlag = True

    d = Device(deviceId)
    openApp(d, deviceId)
    if (d(resourceId = 'com.xingin.xhs:id/cik').wait.exists(timeout = 1000)):
        return {'loginstatus': False, 'status': False}
    waitAndClick(d, 'com.xingin.xhs:id/bo_')
    if userName or userImag:
        waitAndClick(d, 'com.xingin.xhs:id/exy')
        if userName:
            waitAndClickText(d, '名字')
            d(resourceId = 'com.xingin.xhs:id/asz').wait.exists(timeout = 1000)
            d(resourceId = 'com.xingin.xhs:id/asz').set_text(userName)
            waitAndClick(d, 'com.xingin.xhs:id/at8')
            changeNameFlag = d(resourceId = 'com.xingin.xhs:id/asm').text == userName
        if userImag:
            pushFile(deviceId, os.path.split(userImag)[-1], userImag)
            waitAndClick(d, 'com.xingin.xhs:id/avatarLayout')
            if not d(resourceId = 'com.xingin.xhs:id/bky').wait.exists(timeout = 1000):
                changeImagFlag = False
            else:
                waitAndClick(d, 'com.xingin.xhs:id/bky')
                waitAndClick(d, 'com.xingin.xhs:id/afw')
            delFile(deviceId, os.path.split(userImag)[-1])
        waitAndClick(d, 'com.xingin.xhs:id/asr')
    if userPasw:
        waitAndClick(d, 'com.xingin.xhs:id/exz')
        waitAndClickText(d, '账号与安全')
        waitAndClickText(d, '修改密码')
        waitAndClickText(d, '原密码验证')
        print(userOlPs)
        print(userPasw)
        d(text = "输入原密码").set_text(userOlPs)
        time.sleep(0.5)
        d(text = "输入新的密码").set_text(userPasw)
        time.sleep(0.5)
        d(text = "再次输入密码").set_text(userPasw)
        waitAndClick(d, 'com.xingin.xhs:id/cfl')
        changePassFlag = d(text = '修改密码').wait.exists(timeout = 1000)
    os.system('adb -s %s shell am force-stop com.xingin.xhs'%(deviceId))
    return {'status': '1', 'changePassword': changePassFlag, 'changeName': changeNameFlag, 'changeImag': changeImagFlag}

@app.route('/changeCore')
def changeCore():
    '''
    使用前设置：
        语言与输入法->关闭安全键盘（貌似非必须）
        开发者选项中打开[USB调试(安全设置)]（必须设置此项）
    入参：
        device：设备ID
        password：设备密码（切换分身时要用到）（可为空，为空则使用默认密码，默认密码在下方代码中可修改）
    例：
        /changeCore?device=fa16c0f&password=123456
    '''
    deviceId = request.args.get('device', '')
    password = request.args.get('password', '123456') # 括号中第二个参数为默认密码
    d = Device(deviceId)
    appName = 'com.miui.securitycore/com.miui.securityspace.ui.activity.PrivateSpaceMainActivity'
    os.system('adb -s %s shell am start -n %s'%(deviceId, appName))
    time.sleep(1)
    if d(resourceId = 'com.miui.securitycore:id/creteSpace').exists:
        d(resourceId = 'com.miui.securitycore:id/creteSpace').click()
    else:
        d(resourceId = 'android:id/content').scroll(steps=10)
        d(text = "切换分身").click()
        os.system('adb -s %s shell input text %s'%(deviceId, password))
        d(text = '下一步').click()
    return {'status': 1}

if __name__ == "__main__":
    app.run(debug=True)