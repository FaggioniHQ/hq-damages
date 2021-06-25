package com.hqrentalsoftware.damage

import com.hqrentalsoftware.damage.models.Model
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ar.sceneform.collision.Box
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f

class MainActivity : AppCompatActivity() {
    lateinit var arFragment: ArFragment
    private val models = mutableListOf(
        Model(
            R.drawable.chair,
            "Chair",
            R.raw.chair
        ),
        Model(
            R.drawable.oven,
            "Oven",
            R.raw.oven
        ),
        Model(
            R.drawable.chair,
            "Chair",
            R.raw.chair
        ),
        Model(
            R.drawable.oven,
            "Oven",
            R.raw.oven
        ),
    )
    private lateinit var selectedModel: Model
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = fragment as ArFragment
        setupDoubleTapArPlaneListener()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
    }



    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)

        }
    }

    //anchor - reference on the scene
    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        //herarchy - anchor parent - if deleted all children will be deleted
        val anchorNode = AnchorNode(anchor)
        //apply changes to element
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener{
                getCurrentScene().removeChild(anchorNode)
            }
        }
        modelNode.setOnTapListener{_, _ ->
            if(!modelNode.isTransforming){
                if(viewNode.renderable == null){
                    viewNode.renderable = viewRenderable
                }else{
                    viewNode.renderable = null
                }
            }
        }

    }

    private fun getCurrentScene() : Scene{
        return arFragment.arSceneView.scene
    }

    private fun setupDoubleTapArPlaneListener() {
        arFragment.setOnTapArPlaneListener{ hitResult, _, _ ->
            loadModel{ modelRenderable, viewRenderable ->
                addNodeToScene(hitResult.createAnchor(), modelRenderable, viewRenderable)
            }
        }
    }

    //load fun -> always looks like this
    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        //ModelRenderable -> Model Object
        // 3D representation  of a layout -> xml to the scene
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedModel.modelResourceId)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()
        //wait for loading
        CompletableFuture.allOf(
            modelRenderable,
            viewRenderable
        ).thenAccept {
            callback(
                modelRenderable.get(),
                viewRenderable.get()
            )
        }.exceptionally {
            Toast.makeText(this, "Error loading model: $it", Toast.LENGTH_LONG).show()
            null
        }
    }
}