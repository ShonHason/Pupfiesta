import SwiftUI
import Lottie

struct LottieView : UIViewRepresentable {
    let name : String;
    let loopMode : LottieLoopMode;
    var contentMode: UIView.ContentMode = .scaleAspectFit

    func makeUIView(context: Context) -> Lottie.LottieAnimationView {
        let animationView = Lottie.LottieAnimationView(name: name);
        animationView.loopMode = loopMode;
        animationView.contentMode = contentMode;
        animationView.play()
        return animationView;
            
    }
    
    func updateUIView(_ uiView: Lottie.LottieAnimationView, context: Context) {
        uiView.loopMode = loopMode
        uiView.contentMode = contentMode
    }
    
    typealias UIViewType = LottieAnimationView
    
    
}
