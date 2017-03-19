/******************************************************************************
 * Spine Runtimes Software License v2.5
 *
 * Copyright (c) 2013-2016, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable, and
 * non-transferable license to use, install, execute, and perform the Spine
 * Runtimes software and derivative works solely for personal or internal
 * use. Without the written permission of Esoteric Software (see Section 2 of
 * the Spine Software License Agreement), you may not (a) modify, translate,
 * adapt, or develop new applications using the Spine Runtimes or otherwise
 * create derivative works or improvements of the Spine Runtimes or (b) remove,
 * delete, alter, or obscure any trademarks or any copyright, trademark, patent,
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES, BUSINESS INTERRUPTION, OR LOSS OF
 * USE, DATA, OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package spine.starling {
	import spine.Bone;
	import spine.Skeleton;
	import spine.SkeletonData;
	import spine.Slot;
	import spine.atlas.AtlasRegion;
	import spine.attachments.Attachment;
	import spine.attachments.MeshAttachment;
	import spine.attachments.RegionAttachment;

	import starling.display.BlendMode;
	import starling.display.DisplayObject;
	import starling.display.Image;
	import starling.rendering.IndexData;
	import starling.rendering.Painter;
	import starling.rendering.VertexData;
	import starling.utils.Color;
	import starling.utils.MatrixUtil;

	import flash.geom.Matrix;
	import flash.geom.Point;
	import flash.geom.Rectangle;

	public class SkeletonSprite extends DisplayObject {
		static private var _tempPoint : Point = new Point();
		static private var _tempMatrix : Matrix = new Matrix();
		static private var _tempVertices : Vector.<Number> = new Vector.<Number>(8);
		static internal var blendModes : Vector.<String> = new <String>[BlendMode.NORMAL, BlendMode.ADD, BlendMode.MULTIPLY, BlendMode.SCREEN];
		private var _skeleton : Skeleton;
		public var batchable : Boolean = true;
		private var _smoothing : String = "bilinear";
		private static var _twoColorStyle : TwoColorMeshStyle;

		public function SkeletonSprite(skeletonData : SkeletonData) {
			Bone.yDown = true;
			_skeleton = new Skeleton(skeletonData);
			_skeleton.updateWorldTransform();
			if (_twoColorStyle == null) {
				_twoColorStyle = new TwoColorMeshStyle();
			}
		}

		override public function render(painter : Painter) : void {
			painter.state.alpha *= skeleton.color.a;
			var originalBlendMode : String = painter.state.blendMode;
			var r : Number = skeleton.color.r * 255;
			var g : Number = skeleton.color.g * 255;
			var b : Number = skeleton.color.b * 255;
			var drawOrder : Vector.<Slot> = skeleton.drawOrder;
			var worldVertices : Vector.<Number> = _tempVertices;
			var ii : int, iii : int;
			var attachmentColor: spine.Color;
			var rgb : uint, a : Number;
			var dark : uint;
			var mesh : SkeletonMesh;
			var verticesLength : int, verticesCount : int, indicesLength : int;
			var indexData : IndexData, indices : Vector.<uint>, vertexData : VertexData;
			var uvs : Vector.<Number>;

			for (var i : int = 0, n : int = drawOrder.length; i < n; ++i) {
				var slot : Slot = drawOrder[i];
				if (slot.attachment is RegionAttachment) {
					var region : RegionAttachment = slot.attachment as RegionAttachment;
					verticesLength = 4 * 2;
					verticesCount = verticesLength >> 1;
					if (worldVertices.length < verticesLength) worldVertices.length = verticesLength;
					region.computeWorldVertices(slot.bone, worldVertices, 0, 2);					

					mesh = region.rendererObject as SkeletonMesh;
					if (mesh == null) {
						if (region.rendererObject is Image)
							region.rendererObject = mesh = new SkeletonMesh(Image(region.rendererObject).texture);
						if (region.rendererObject is AtlasRegion)
							region.rendererObject = mesh = new SkeletonMesh(Image(AtlasRegion(region.rendererObject).rendererObject).texture);
						
						indexData = mesh.getIndexData();
						indices = new <uint>[0, 1, 2, 2, 3, 0];
						for (ii = 0; ii < indices.length; ii++)
							indexData.setIndex(ii, indices[ii]);
						indexData.numIndices = 6;
						indexData.trim();
					}
					
					attachmentColor = region.color;
					uvs = region.uvs;													
				} else if (slot.attachment is MeshAttachment) {
					var meshAttachment : MeshAttachment = MeshAttachment(slot.attachment);
					verticesLength = meshAttachment.worldVerticesLength;
					verticesCount = verticesLength >> 1;
					if (worldVertices.length < verticesLength) worldVertices.length = verticesLength;
					meshAttachment.computeWorldVertices(slot, 0, meshAttachment.worldVerticesLength, worldVertices, 0, 2);
					
					mesh = meshAttachment.rendererObject as SkeletonMesh;
					if (mesh == null) {
						if (meshAttachment.rendererObject is Image)
							meshAttachment.rendererObject = mesh = new SkeletonMesh(Image(meshAttachment.rendererObject).texture);
						if (meshAttachment.rendererObject is AtlasRegion)
							meshAttachment.rendererObject = mesh = new SkeletonMesh(Image(AtlasRegion(meshAttachment.rendererObject).rendererObject).texture);						
						mesh.setStyle(_twoColorStyle);
						
						indexData = mesh.getIndexData();
						indices = meshAttachment.triangles;
						indicesLength = meshAttachment.triangles.length;
						for (ii = 0; ii < indicesLength; ii++) {
							indexData.setIndex(ii, indices[ii]);
						}
						indexData.numIndices = indicesLength;
						indexData.trim();
					}					
										
					attachmentColor = meshAttachment.color;
					uvs = meshAttachment.uvs;					
				}
				
				a = slot.color.a * attachmentColor.a;
				rgb = Color.rgb(r * slot.color.r * attachmentColor.r, g * slot.color.g * attachmentColor.g, b * slot.color.b * attachmentColor.b);
				if (slot.darkColor == null) dark = Color.rgb(0, 0, 0);
				else dark = Color.rgb(slot.darkColor.r * 255, slot.darkColor.g * 255, slot.darkColor.b * 255);	

				// Mesh doesn't retain the style, can't find the reason why
				mesh.setStyle(_twoColorStyle);			
				vertexData = mesh.getVertexData();					
				vertexData.colorize("color", rgb, a);
				vertexData.colorize("color2", dark);
				for (ii = 0, iii = 0; ii < verticesCount; ii++, iii += 2) {
					mesh.setVertexPosition(ii, worldVertices[iii], worldVertices[iii + 1]);
					mesh.setTexCoords(ii, uvs[iii], uvs[iii + 1]);
				}
				vertexData.numVertices = verticesCount;
				painter.state.blendMode = blendModes[slot.data.blendMode.ordinal];				
				painter.batchMesh(mesh);
			}
			painter.state.blendMode = originalBlendMode;
		}

		override public function hitTest(localPoint : Point) : DisplayObject {
			var minX : Number = Number.MAX_VALUE, minY : Number = Number.MAX_VALUE;
			var maxX : Number = -Number.MAX_VALUE, maxY : Number = -Number.MAX_VALUE;
			var slots : Vector.<Slot> = skeleton.slots;
			var worldVertices : Vector.<Number> = _tempVertices;
			var empty : Boolean = true;
			for (var i : int = 0, n : int = slots.length; i < n; ++i) {
				var slot : Slot = slots[i];
				var attachment : Attachment = slot.attachment;
				if (!attachment) continue;
				var verticesLength : int;
				if (attachment is RegionAttachment) {
					var region : RegionAttachment = RegionAttachment(slot.attachment);
					verticesLength = 8;
					region.computeWorldVertices(slot.bone, worldVertices, 0, 2);
				} else if (attachment is MeshAttachment) {
					var mesh : MeshAttachment = MeshAttachment(attachment);
					verticesLength = mesh.worldVerticesLength;
					if (worldVertices.length < verticesLength) worldVertices.length = verticesLength;
					mesh.computeWorldVertices(slot, 0, verticesLength, worldVertices, 0, 2);
				} else
					continue;

				if (verticesLength != 0)
					empty = false;

				for (var ii : int = 0; ii < verticesLength; ii += 2) {
					var x : Number = worldVertices[ii], y : Number = worldVertices[ii + 1];
					minX = minX < x ? minX : x;
					minY = minY < y ? minY : y;
					maxX = maxX > x ? maxX : x;
					maxY = maxY > y ? maxY : y;
				}
			}

			if (empty)
				return null;

			var temp : Number;
			if (maxX < minX) {
				temp = maxX;
				maxX = minX;
				minX = temp;
			}
			if (maxY < minY) {
				temp = maxY;
				maxY = minY;
				minY = temp;
			}

			if (localPoint.x >= minX && localPoint.x < maxX && localPoint.y >= minY && localPoint.y < maxY)
				return this;

			return null;
		}

		override public function getBounds(targetSpace : DisplayObject, resultRect : Rectangle = null) : Rectangle {
			if (!resultRect)
				resultRect = new Rectangle();
			if (targetSpace == this)
				resultRect.setTo(0, 0, 0, 0);
			else if (targetSpace == parent)
				resultRect.setTo(x, y, 0, 0);
			else {
				getTransformationMatrix(targetSpace, _tempMatrix);
				MatrixUtil.transformCoords(_tempMatrix, 0, 0, _tempPoint);
				resultRect.setTo(_tempPoint.x, _tempPoint.y, 0, 0);
			}
			return resultRect;
		}

		public function get skeleton() : Skeleton {
			return _skeleton;
		}

		public function get smoothing() : String {
			return _smoothing;
		}

		public function set smoothing(smoothing : String) : void {
			_smoothing = smoothing;
		}
	}
}