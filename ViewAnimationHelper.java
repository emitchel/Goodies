import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout.LayoutParams;

/**
 * Simple class to help with view animations
 * 
 * @author elliot-mitchell
 * 
 */
public class ViewAnimationHelper {

	public static void animateHeight(final View view, int from, int to,
			int duration) {

		ValueAnimator anim = ValueAnimator.ofInt(from, to);
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				int val = (Integer) valueAnimator.getAnimatedValue();
				ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
				layoutParams.height = val;
				view.setLayoutParams(layoutParams);
			}

		});
		anim.setDuration(duration);
		anim.start();
	}

	/**
	 * Easy way to expand a given view after measuring with
	 * v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	 * 
	 * @param v
	 * @param duration
	 */
	public static void expand(final View v, int duration,
			boolean bStartFromZeroHeight) {
		v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		final int targetHeight = v.getMeasuredHeight();

		if (bStartFromZeroHeight)
			v.getLayoutParams().height = 0;

		v.setVisibility(View.VISIBLE);
		Animation a = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				v.getLayoutParams().height = interpolatedTime == 1 ? LayoutParams.WRAP_CONTENT
						: (int) (targetHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration(duration);
		v.startAnimation(a);
	}

	/**
	 * Easy way to just collapse any given view and any given speed
	 * 
	 * @param v
	 * @param duration
	 */
	public static void collapse(final View v, int duration) {
		final int initialHeight = v.getMeasuredHeight();

		Animation a = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime,
					Transformation t) {
				if (interpolatedTime == 1) {
					v.setVisibility(View.GONE);
				} else {
					v.getLayoutParams().height = initialHeight
							- (int) (initialHeight * interpolatedTime);
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration(duration);
		v.startAnimation(a);
	}

	/**
	 * 
	 * @param v
	 *            : view you want to fade in/out
	 * @param bFadeIn
	 *            : true if you're fading in , false if you're fading out
	 * @param animationListener
	 *            : listener for the animation, generally you care about the
	 *            onAnimationEnd(Animator animation) method
	 */
	public static void fadeView(View v, boolean bFadeIn,
			AnimatorListenerAdapter animationListener, int duration) {
		
		v.setVisibility(View.VISIBLE);
		
		if(bFadeIn)
			v.setAlpha(0f);
		else
			v.setAlpha(1f);
		
		v.animate().alpha((bFadeIn ? 1f : 0f)).setDuration(duration)
				.setListener(animationListener);
	}

    
    public static void expandIfCollapsed(View v, int duration, int targetHeight) {
        expandIfCollapsed(v, duration, targetHeight, DEFAULT_COLLAPSED_HEIGHT);
    }

    public static void expandIfCollapsed(View v, int duration, int targetHeight, int collapsedHeight) {
        if (v.getHeight() <= collapsedHeight) {
            expand(v, duration, targetHeight);
        }
    }

    public static void expand(final View v, int duration, int targetHeight) {
        int prevHeight = v.getHeight();

        v.setVisibility(View.VISIBLE);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void collapseIfExpanded(View v, int duration, int targetHeight) {
        if (v.getHeight() != 0) {
            collapse(v, duration, targetHeight);
        }
    }

    public static void collapse(final View v, int duration, final int targetHeight) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(View.GONE);
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }

        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void expandIfCollapsed(final View v, int duration, int measureWidthParams, int measureHeightParams,
                                         boolean shouldStartFromZeroHeight) {
        if (v.getHeight() == 0) {
            expand(v, duration, measureWidthParams, measureHeightParams, shouldStartFromZeroHeight);
        }
    }

    /**
     * @param v                         View to Expand
     * @param duration                  Duration of animation
     * @param measureWidthParams        Pass in the layout params in which you want to measure e.g. LinearLayout.LayoutParams.MATCH_PARENT
     * @param measureHeightParams       Pass in the layout params in which you want to measure e.g. LinearLayout.LayoutParams.WRAP_CONTENT
     * @param shouldStartFromZeroHeight If you want the view to initially be 0 height
     */
    public static void expand(final View v, int duration, int measureWidthParams, int measureHeightParams,
                              boolean shouldStartFromZeroHeight) {


        v.measure(measureWidthParams, measureHeightParams);
        final int targetHeight = v.getMeasuredHeight();

        if (shouldStartFromZeroHeight)
            v.getLayoutParams().height = 0;

        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime,
                                               Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1 ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(duration);
        v.requestLayout(); // invalidate to avoid random ui bugs
        v.startAnimation(a);
    }

    /**
     * Easy way to just collapse any given view and any given speed
     *
     * @param v        View to collapse
     * @param duration Duration of animation
     */
    public static void collapseToZero(final View v, int duration) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime,
                                               Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight
                            - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration(duration);
        v.startAnimation(a);
    }

}
