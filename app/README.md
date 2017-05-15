![Alt text](./device-2017-05-15-142922.gif)

**PinWheelWidget** include two base widget : the simplification **CircleProgressBar** and **ParticleSystem**.

##PinWheelWidget
 Before use it ,must call PinWheelWidget#init() to beginning. call setMax to set totla degree , setProgress to set current degree, start() to start animator,it will be continue util call stop, in the en of use it ,call free()
That's all ,enjoy it.

##ParticleSystem
particle emit system

##CicileProgressBar

It supports ProgerssBar's function with the shape of circle.
it's also supports a arc on display,setStartDegree(int) to start scale ,setTotalDegree(int) to calculate the end of arc.
####The Center of Circle:
 The center of circle area can be use to draw custom view via extends it and implements it's Method drawCustomView(Canvas, float, float, float)


