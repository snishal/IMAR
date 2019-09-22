/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.learners.imar;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.learners.imar.helpers.FirebaseManager;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.core.Config;
import com.google.ar.core.Config.CloudAnchorMode;
import com.google.ar.core.Session;
import com.google.ar.core.learners.imar.helpers.CloudAnchorManager;
import com.google.ar.core.learners.imar.helpers.SnackbarHelper;
import com.google.ar.core.Anchor.CloudAnchorState;



/**
 * Main Fragment for the Cloud Anchors Codelab.
 *
 * <p>This is where the AR Session and the Cloud Anchors are managed.
 */
public class CloudAnchorFragment extends ArFragment {

  private Scene arScene;
  private AnchorNode anchorNode;
  private ModelRenderable andyRenderable;
  private final CloudAnchorManager cloudAnchorManager = new CloudAnchorManager();
  private final SnackbarHelper snackbarHelper = new SnackbarHelper();
  private Button p1;
  private Button p2;
  private Button nextButton;
  private FirebaseManager firebaseManager;
  private int s1 = 142, l1 = 151;
  private int s2 = 152, l2 = 174;

  private synchronized void onShortCodeEntered(int shortCode) {
    firebaseManager.getCloudAnchorId(shortCode, cloudAnchorId -> {
      if (cloudAnchorId == null || cloudAnchorId.isEmpty()) {
        snackbarHelper.showMessage(
                getActivity(),
                "No further path.");
        return;
      }
      cloudAnchorManager.resolveCloudAnchor(
              getArSceneView().getSession(),
              cloudAnchorId,
              anchor -> onResolvedAnchorAvailable(anchor, shortCode));
      nextButton.setEnabled(true);
    });
  }

  private synchronized void onResolvedAnchorAvailable(Anchor anchor, int shortCode) {
    CloudAnchorState cloudState = anchor.getCloudAnchorState();
    if (cloudState == CloudAnchorState.SUCCESS) {
      snackbarHelper.showMessage(getActivity(), "Proceed Further");
      setNewAnchor(anchor);
    } else {
      snackbarHelper.showMessage(
              getActivity(),
              "Error while resolving anchor with short code "
                      + shortCode
                      + ". Error: "
                      + cloudState.toString());
//      resolveButton.setEnabled(true);
    }
  }

  private synchronized void startP1() {
    p1.setEnabled(false);
    onShortCodeEntered(s1);
    nextButton.setEnabled(true);
  }

  private synchronized void startP2() {
    p2.setEnabled(false);
    onShortCodeEntered(s2);
    nextButton.setEnabled(true);
  }

  private synchronized void onNextButtonPressed(){
      nextButton.setEnabled(false);
      if(p2.isEnabled()){
        s1++;
        if(s1 == l1){
          snackbarHelper.showMessage(getActivity(), "Reached Pantry.");
          nextButton.setEnabled(false);
        }else{
          onShortCodeEntered(s1);
        }
      }else{
        s2++;
        if(s2 == l2){
          snackbarHelper.showMessage(getActivity(), "Reached Pantry.");
          nextButton.setEnabled(false);
        }else{
          onShortCodeEntered(s2);
        }
      }

  }

  private synchronized void onHostedAnchorAvailable(Anchor anchor) {
    CloudAnchorState cloudState = anchor.getCloudAnchorState();
    if (cloudState == CloudAnchorState.SUCCESS) {
      String cloudAnchorId = anchor.getCloudAnchorId();
      firebaseManager.nextShortCode(shortCode -> {
        if (shortCode != null) {
          firebaseManager.storeUsingShortCode(shortCode, cloudAnchorId);
          snackbarHelper
                  .showMessage(getActivity(), "Cloud Anchor Hosted. Short code: " + shortCode);
        } else {
          // Firebase could not provide a short code.
          snackbarHelper
                  .showMessage(getActivity(), "Cloud Anchor Hosted, but could not "
                          + "get a short code from Firebase.");
        }
      });
      setNewAnchor(anchor);
    } else {
      snackbarHelper.showMessage(getActivity(), "Error while hosting: " + cloudState.toString());
    }
  }

  @Override
  protected Config getSessionConfiguration(Session session) {
    Config config = super.getSessionConfiguration(session);
    config.setCloudAnchorMode(CloudAnchorMode.ENABLED);
    return config;
  }

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void onAttach(Context context) {
    super.onAttach(context);
    ModelRenderable.builder()
        .setSource(context, Uri.parse("arrow.sfb"))
        .build()
        .thenAccept(renderable -> andyRenderable = renderable);
    firebaseManager = new FirebaseManager(context);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    // Inflate from the Layout XML file.
    View rootView = inflater.inflate(R.layout.cloud_anchor_fragment, container, false);
    LinearLayout arContainer = rootView.findViewById(R.id.ar_container);

    // Call the ArFragment's implementation to get the AR View.
    View arView = super.onCreateView(inflater, arContainer, savedInstanceState);
    arContainer.addView(arView);

//    Button clearButton = rootView.findViewById(R.id.clear_button);
//    clearButton.setOnClickListener(v -> onClearButtonPressed());

    p1 = rootView.findViewById(R.id.p1);
    p1.setOnClickListener(v -> startP1());

    p2 = rootView.findViewById(R.id.p2);
    p2.setOnClickListener(v -> startP2());

    nextButton = rootView.findViewById(R.id.next_button);
    nextButton.setOnClickListener(v -> onNextButtonPressed());
    nextButton.setEnabled(false);

    arScene = getArSceneView().getScene();
    arScene.addOnUpdateListener(frameTime -> cloudAnchorManager.onUpdate());
    setOnTapArPlaneListener((hitResult, plane, motionEvent) -> onArPlaneTap(hitResult));
    return rootView;
  }

  private synchronized void onArPlaneTap(HitResult hitResult) {
    if (anchorNode != null) {
      // Do nothing if there was already an anchor in the Scene.
      return;
    }
    Anchor anchor = hitResult.createAnchor();
    setNewAnchor(anchor);
//    resolveButton.setEnabled(false);

    snackbarHelper.showMessage(getActivity(), "Now hosting anchor...");
    cloudAnchorManager.hostCloudAnchor(getArSceneView().getSession(), anchor, this::onHostedAnchorAvailable);
  }

  private synchronized void onClearButtonPressed() {
    // Clear the anchor from the scene.
    cloudAnchorManager.clearListeners();
//    resolveButton.setEnabled(true);
    setNewAnchor(null);
  }

  // Modify the renderables when a new anchor is available.
  private synchronized void setNewAnchor(@Nullable Anchor anchor) {
    if (anchorNode != null) {
      arScene.removeChild(anchorNode);
      anchorNode = null;
    }
    if (anchor != null) {
      if (andyRenderable == null) {
        // Display an error message if the renderable model was not available.
        Toast toast = Toast.makeText(getContext(), "Andy model was not loaded.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        return;
      }
      // Create the Anchor.
      anchorNode = new AnchorNode(anchor);
      arScene.addChild(anchorNode);

      // Create the transformable andy and add it to the anchor.
      TransformableNode andy = new TransformableNode(getTransformationSystem());
      andy.setParent(anchorNode);
      andy.setRenderable(andyRenderable);
      andy.select();
    }
  }
}
