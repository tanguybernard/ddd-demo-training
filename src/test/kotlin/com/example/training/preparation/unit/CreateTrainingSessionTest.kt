package com.example.training.preparation.unit

import com.example.training.preparation.application.course.CreateSessionCommand
import com.example.training.preparation.application.course.CreateTrainingSession
import com.example.training.preparation.application.course.SessionPeriodIsIncorrectToTheDurationOfACourse
import com.example.training.preparation.application.course.TrainingSessionDomainService
import com.example.training.preparation.domain.course.TrainingCourse
import com.example.training.preparation.domain.course.TrainingCourseRepository
import com.example.training.preparation.domain.course.CourseId
import com.example.training.preparation.domain.course.TrainingName
import com.example.training.preparation.domain.trainer.Trainer
import com.example.training.preparation.domain.trainer.TrainerBuilder
import com.example.training.preparation.domain.trainer.TrainerCannotLeadThisTraining
import com.example.training.preparation.domain.trainer.TrainerRepository
import com.example.training.preparation.infrastructure.stubs.InMemoryTrainerRepository
import com.example.training.preparation.infrastructure.stubs.InMemoryTrainingRepository
import com.example.training.preparation.infrastructure.stubs.TrainingCourseDoesNotExist
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

class CreateTrainingSessionTest {


    private lateinit var trainingCourseRepository: TrainingCourseRepository
    private lateinit var trainerRepository: TrainerRepository
    private lateinit var trainer: Trainer
    private lateinit var trainingId: String

    @BeforeEach
    fun setUp() {
        this.trainingCourseRepository = InMemoryTrainingRepository()
        this.trainerRepository = InMemoryTrainerRepository()



        this.trainingId = UUID.randomUUID().toString()
        trainingCourseRepository.createTrainingCourse(
            TrainingCourse(
                CourseId(this.trainingId),
                TrainingName("DDD Training"),
                3
            ))

        trainer = TrainerBuilder().withExpertises(listOf( CourseId(trainingId))).build()

        this.trainerRepository.add(
            trainer
        )
    }


    @Test
    fun createTrainingSession(){

        val useCase = CreateTrainingSession(
            trainingSessionDomainService = TrainingSessionDomainService(
                trainerRepository,
                trainingCourseRepository
            )
        )
        val startDate = LocalDate.now().plusDays(3)
        val endDate = startDate.plusDays(3)
        val sessionId = useCase.execute(
            CreateSessionCommand(startDate, endDate, this.trainingId, trainer.trainerId.value)
        )

        assertThat(trainingCourseRepository
            .getTrainingCourseBy(CourseId(this.trainingId))
            .getSessionBy(sessionId).sessionId) //TODO Demeter law simplify
            .isEqualTo(sessionId)

    }


    @Test
    fun `create a session where period does not match with the duration of the course`(){

        val useCase = CreateTrainingSession(
            trainingSessionDomainService = TrainingSessionDomainService(
                trainerRepository,
                trainingCourseRepository
            )
        )
        val startDate = LocalDate.now().plusDays(3)
        val endDate = startDate.plusDays(2)

        assertThrows<SessionPeriodIsIncorrectToTheDurationOfACourse> {
            useCase.execute(CreateSessionCommand(
                startDate, endDate, this.trainingId, trainer.trainerId.value)
            )
        }
    }

    @Test
    fun `create a session for Training course that no longer exists`(){
        val useCase = CreateTrainingSession(
            trainingSessionDomainService = TrainingSessionDomainService(
                trainerRepository,
                trainingCourseRepository
            )
        )
        val startDate = LocalDate.now().plusDays(3)
        val endDate = startDate.plusDays(3)

        assertThrows<TrainingCourseDoesNotExist> {
            useCase.execute(CreateSessionCommand(
                startDate, endDate, "IdDoesNotExist", trainer.trainerId.value)
            )
        }
    }

    @Test
    fun `a trainer cannot lead a Training if he,she have no expertise on it`(){
        val useCase = CreateTrainingSession(
            trainingSessionDomainService = TrainingSessionDomainService(
                trainerRepository,
                trainingCourseRepository
            )
        )
        val startDate = LocalDate.now().plusDays(3)
        val endDate = startDate.plusDays(3)

        trainer.setExpertises(listOf())

        trainerRepository.update(trainer)

        assertThrows<TrainerCannotLeadThisTraining> {
            useCase.execute(CreateSessionCommand(
                startDate, endDate, this.trainingId, trainer.trainerId.value)
            )
        }
    }
}
