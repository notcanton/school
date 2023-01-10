#pragma once

#include <string>
#include <vector>
#include <iostream>
#include <algorithm>

#define GLM_ENABLE_EXPERIMENTAL
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>	

class Bone_Animation
{
public:
	Bone_Animation();
	~Bone_Animation();

	void init();
	glm::mat4 transform(int index);
	void update(float delta_time);
	void reset();

public:

	// Here the head of each vector is the root bone
	std::vector<glm::vec3> scale_vector;
	std::vector<glm::vec3> rotation_degree_vector;
	std::vector<glm::vec4> colors;

	std::vector<glm::vec3> translate_vector;
	std::vector<glm::vec3> placement_vector;
	float obj_height = 0.5f;
	float X1 = 0.0f;
	float Y1 = 0.0f;
	float Z1 = 0.0f;
	float X2 = 0.0f;
	float Y2 = 0.0f;
	float Z2 = 0.0f;
	float X3 = 0.0f;
	float Y3 = 0.0f;
	float Z3 = 0.0f;

	glm::vec3 root_position;

};

